package com.korpay.billpay.service.webhook;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korpay.billpay.domain.entity.WebhookLog;
import com.korpay.billpay.domain.enums.WebhookLogStatus;
import com.korpay.billpay.repository.WebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookLoggingService {

    private final WebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public WebhookLog logWebhookReceived(
            Long pgConnectionId,
            String eventType,
            String rawBody,
            Map<String, String> headers,
            String signature,
            boolean signatureVerified) {

        Map<String, Object> payload = parsePayload(rawBody);

        WebhookLog webhookLog = WebhookLog.builder()
                .pgConnectionId(pgConnectionId)
                .eventType(eventType)
                .payload(payload)
                .headers(headers)
                .signature(signature)
                .signatureVerified(signatureVerified)
                .status(WebhookLogStatus.RECEIVED)
                .build();

        return webhookLogRepository.save(webhookLog);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToProcessing(UUID webhookLogId) {
        webhookLogRepository.findById(webhookLogId).ifPresent(webhookLog -> {
            webhookLog.markProcessing();
            webhookLogRepository.save(webhookLog);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToProcessed(UUID webhookLogId, UUID transactionId, UUID transactionEventId) {
        webhookLogRepository.findById(webhookLogId).ifPresent(webhookLog -> {
            webhookLog.markProcessed(transactionId, transactionEventId);
            webhookLogRepository.save(webhookLog);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToFailed(UUID webhookLogId, String errorMessage) {
        webhookLogRepository.findById(webhookLogId).ifPresent(webhookLog -> {
            webhookLog.markFailed(errorMessage);
            webhookLogRepository.save(webhookLog);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateToIgnored(UUID webhookLogId, String reason) {
        webhookLogRepository.findById(webhookLogId).ifPresent(webhookLog -> {
            webhookLog.markIgnored(reason);
            webhookLogRepository.save(webhookLog);
        });
    }

    private Map<String, Object> parsePayload(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse webhook payload as JSON, storing as raw string: {}", e.getMessage());
            return Map.of("raw", rawBody);
        }
    }
}
