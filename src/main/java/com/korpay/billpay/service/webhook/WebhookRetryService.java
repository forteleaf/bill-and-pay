package com.korpay.billpay.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korpay.billpay.domain.entity.WebhookLog;
import com.korpay.billpay.domain.enums.WebhookLogStatus;
import com.korpay.billpay.repository.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRetryService {

    private final WebhookLogRepository webhookLogRepository;
    private final WebhookProcessingService webhookProcessingService;
    private final ObjectMapper objectMapper;

    @Value("${webhook.retry.max-attempts:5}")
    private int maxRetryAttempts;

    @Value("${webhook.retry.enabled:true}")
    private boolean retryEnabled;

    @Scheduled(fixedDelayString = "${webhook.retry.interval-ms:60000}")
    @Transactional
    public void processFailedWebhooks() {
        if (!retryEnabled) {
            return;
        }

        List<WebhookLog> failedWebhooks = webhookLogRepository.findRetryableWebhooks(
                WebhookLogStatus.FAILED,
                maxRetryAttempts
        );

        if (failedWebhooks.isEmpty()) {
            return;
        }

        log.info("Found {} failed webhooks to retry", failedWebhooks.size());

        for (WebhookLog webhookLog : failedWebhooks) {
            retryWebhook(webhookLog);
        }
    }

    private void retryWebhook(WebhookLog webhookLog) {
        try {
            log.info("Retrying webhook: id={}, pgConnectionId={}, attempt={}",
                    webhookLog.getId(), webhookLog.getPgConnectionId(), webhookLog.getRetryCount() + 1);

            String rawBody = extractRawBody(webhookLog);
            Map<String, String> headers = webhookLog.getHeaders() != null ? webhookLog.getHeaders() : new HashMap<>();

            webhookProcessingService.processWebhook(
                    webhookLog.getEventType(),
                    webhookLog.getPgConnectionId(),
                    extractWebhookSecret(headers),
                    rawBody,
                    headers
            );

            log.info("Webhook retry successful: id={}", webhookLog.getId());

        } catch (Exception e) {
            log.error("Webhook retry failed: id={}, error={}", webhookLog.getId(), e.getMessage());
            webhookLog.markFailed(e.getMessage());
            webhookLogRepository.save(webhookLog);
        }
    }

    private String extractRawBody(WebhookLog webhookLog) {
        try {
            Map<String, Object> payload = webhookLog.getPayload();
            if (payload.containsKey("raw")) {
                return (String) payload.get("raw");
            }
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to extract raw body from webhook log: {}", e.getMessage());
            return "";
        }
    }

    private String extractWebhookSecret(Map<String, String> headers) {
        return headers.getOrDefault("X-Webhook-Secret", headers.getOrDefault("x-webhook-secret", ""));
    }
}
