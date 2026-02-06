package com.korpay.billpay.service.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.korpay.billpay.config.tenant.TenantContextHolder;
import com.korpay.billpay.config.tenant.TenantService;
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

/**
 * 웹훅 재처리 서비스.
 *
 * 실패한 웹훅을 주기적으로 스캔하여 재전송을 시도한다.
 * 모든 활성 테넌트를 순회하며, 테넌트별로 최대 재시도 횟수 이내의
 * FAILED 상태 웹훅을 대상으로 재처리를 수행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRetryService {

    private final WebhookLogRepository webhookLogRepository;
    private final WebhookProcessingService webhookProcessingService;
    private final TenantService tenantService;
    private final ObjectMapper objectMapper;

    /** 웹훅 최대 재시도 횟수 (기본값: 5회) */
    @Value("${webhook.retry.max-attempts:5}")
    private int maxRetryAttempts;

    /** 재시도 기능 활성화 여부 (기본값: true) */
    @Value("${webhook.retry.enabled:true}")
    private boolean retryEnabled;

    /**
     * 실패한 웹훅을 주기적으로 재처리한다.
     * 모든 활성 테넌트를 순회하며 각 테넌트 컨텍스트 내에서 재시도를 수행한다.
     * 실행 주기는 webhook.retry.interval-ms 설정값에 따르며, 기본 60초이다.
     */
    @Scheduled(fixedDelayString = "${webhook.retry.interval-ms:60000}")
    public void processFailedWebhooks() {
        if (!retryEnabled) {
            return;
        }

        List<String> activeTenants = tenantService.getAllActiveTenants();

        for (String tenantId : activeTenants) {
            TenantContextHolder.runInTenant(tenantId, () -> processFailedWebhooksForTenant(tenantId));
        }
    }

    /**
     * 특정 테넌트의 실패 웹훅을 조회하여 재처리한다.
     * 최대 재시도 횟수를 초과하지 않은 FAILED 상태의 웹훅만 대상으로 한다.
     */
    @Transactional
    protected void processFailedWebhooksForTenant(String tenantId) {
        List<WebhookLog> failedWebhooks = webhookLogRepository.findRetryableWebhooks(
                WebhookLogStatus.FAILED,
                maxRetryAttempts
        );

        if (failedWebhooks.isEmpty()) {
            return;
        }

        log.info("Found {} failed webhooks to retry for tenant {}", failedWebhooks.size(), tenantId);

        for (WebhookLog webhookLog : failedWebhooks) {
            retryWebhook(webhookLog);
        }
    }

    /**
     * 개별 웹훅을 재전송한다.
     * 원본 payload와 헤더를 복원하여 WebhookProcessingService로 재처리를 위임한다.
     * 재시도 실패 시 실패 사유를 기록하고 retry count를 증가시킨다.
     */
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

    /**
     * WebhookLog의 payload에서 원본 요청 body를 추출한다.
     * payload에 "raw" 키가 있으면 원본 문자열을 반환하고,
     * 없으면 payload 전체를 JSON 문자열로 직렬화하여 반환한다.
     */
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

    /**
     * 헤더에서 웹훅 시크릿 값을 추출한다.
     * 대소문자 모두 확인하여 X-Webhook-Secret 헤더 값을 반환한다.
     */
    private String extractWebhookSecret(Map<String, String> headers) {
        return headers.getOrDefault("X-Webhook-Secret", headers.getOrDefault("x-webhook-secret", ""));
    }
}
