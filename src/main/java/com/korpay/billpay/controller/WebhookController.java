package com.korpay.billpay.controller;

import com.korpay.billpay.dto.webhook.WebhookResponse;
import com.korpay.billpay.exception.webhook.SignatureVerificationFailedException;
import com.korpay.billpay.exception.webhook.WebhookProcessingException;
import com.korpay.billpay.service.webhook.WebhookProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookProcessingService webhookProcessingService;

    @PostMapping("/{pgCode}")
    public ResponseEntity<WebhookResponse> receiveWebhook(
            @PathVariable String pgCode,
            @RequestParam(required = false) UUID pgConnectionId,
            @RequestParam(required = false) String webhookSecret,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawBody) {

        log.info("Received webhook request for PG: {}, Connection ID: {}", pgCode, pgConnectionId);

        if (pgConnectionId == null) {
            log.error("Missing required parameter: pgConnectionId");
            return ResponseEntity.badRequest()
                    .body(WebhookResponse.error("Missing pgConnectionId parameter"));
        }

        if (webhookSecret == null) {
            log.error("Missing required parameter: webhookSecret");
            return ResponseEntity.badRequest()
                    .body(WebhookResponse.error("Missing webhookSecret parameter"));
        }

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
