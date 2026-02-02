package com.korpay.billpay.controller;

import com.korpay.billpay.config.tenant.TenantContextHolder;
import com.korpay.billpay.config.tenant.TenantService;
import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.dto.webhook.WebhookResponse;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.InvalidTenantException;
import com.korpay.billpay.exception.TenantNotFoundException;
import com.korpay.billpay.exception.webhook.SignatureVerificationFailedException;
import com.korpay.billpay.exception.webhook.WebhookProcessingException;
import com.korpay.billpay.repository.PgConnectionRepository;
import com.korpay.billpay.service.webhook.WebhookProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private static final String TENANT_ID_PATTERN = "^[a-z][a-z0-9_]{2,49}$";

    private final WebhookProcessingService webhookProcessingService;
    private final TenantService tenantService;
    private final PgConnectionRepository pgConnectionRepository;

    @PostMapping("/{tenantId}/{pgCode}")
    public ResponseEntity<WebhookResponse> receiveWebhookWithTenant(
            @PathVariable String tenantId,
            @PathVariable String pgCode,
            @RequestParam UUID pgConnectionId,
            @RequestParam String webhookSecret,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawBody) {

        log.info("Received tenant-aware webhook: tenant={}, pgCode={}, connectionId={}", 
                tenantId, pgCode, pgConnectionId);

        if (!tenantId.matches(TENANT_ID_PATTERN)) {
            log.warn("Invalid tenant ID format: {}", tenantId);
            return ResponseEntity.badRequest()
                    .body(WebhookResponse.error("Invalid tenant ID format"));
        }

        try {
            tenantService.validateTenantExists(tenantId);
        } catch (TenantNotFoundException e) {
            log.warn("Tenant not found: {}", tenantId);
            return ResponseEntity.badRequest()
                    .body(WebhookResponse.error("Tenant not found: " + tenantId));
        }

        return TenantContextHolder.runInTenant(tenantId, 
            (java.util.function.Supplier<ResponseEntity<WebhookResponse>>) () -> 
                processWebhook(pgCode, pgConnectionId, webhookSecret, rawBody, headers)
        );
    }

    @PostMapping("/{pgCode}")
    public ResponseEntity<WebhookResponse> receiveWebhookLegacy(
            @PathVariable String pgCode,
            @RequestParam UUID pgConnectionId,
            @RequestParam String webhookSecret,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawBody) {

        log.warn("DEPRECATED: Using legacy webhook endpoint without tenant ID. " +
                "Please update to /webhook/{tenantId}/{pgCode}. pgCode={}, connectionId={}", 
                pgCode, pgConnectionId);

        PgConnection pgConnection = pgConnectionRepository.findById(pgConnectionId)
                .orElse(null);

        if (pgConnection == null) {
            log.error("PG Connection not found: {}", pgConnectionId);
            return ResponseEntity.badRequest()
                    .body(WebhookResponse.error("PG Connection not found"));
        }

        String tenantId = pgConnection.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            log.error("PG Connection has no tenant ID assigned: {}", pgConnectionId);
            return ResponseEntity.badRequest()
                    .body(WebhookResponse.error("PG Connection has no tenant ID assigned. Please configure tenant_id."));
        }

        return TenantContextHolder.runInTenant(tenantId, 
            (java.util.function.Supplier<ResponseEntity<WebhookResponse>>) () -> 
                processWebhook(pgCode, pgConnectionId, webhookSecret, rawBody, headers)
        );
    }

    private ResponseEntity<WebhookResponse> processWebhook(
            String pgCode,
            UUID pgConnectionId,
            String webhookSecret,
            String rawBody,
            Map<String, String> headers) {

        try {
            WebhookResponse response = webhookProcessingService.processWebhook(
                    pgCode,
                    pgConnectionId,
                    webhookSecret,
                    rawBody,
                    headers
            );
            return ResponseEntity.ok(response);

        } catch (SignatureVerificationFailedException e) {
            log.warn("Webhook signature verification failed for PG: {}, error: {}", pgCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(WebhookResponse.error("Signature verification failed"));

        } catch (WebhookProcessingException e) {
            log.error("Webhook processing failed for PG: {}", pgCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebhookResponse.error("Internal processing error"));

        } catch (Exception e) {
            log.error("Unexpected error processing webhook for PG: {}", pgCode, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WebhookResponse.error("Internal server error"));
        }
    }
}
