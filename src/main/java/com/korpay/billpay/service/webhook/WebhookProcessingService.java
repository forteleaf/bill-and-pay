package com.korpay.billpay.service.webhook;

import com.korpay.billpay.domain.entity.MerchantPgMapping;
import com.korpay.billpay.domain.entity.Transaction;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.entity.UnmappedTransaction;
import com.korpay.billpay.domain.entity.WebhookIdempotencyKey;
import com.korpay.billpay.domain.entity.WebhookLog;
import com.korpay.billpay.dto.webhook.TransactionDto;
import com.korpay.billpay.dto.webhook.WebhookResponse;
import com.korpay.billpay.exception.webhook.DuplicateTransactionException;
import com.korpay.billpay.exception.webhook.MerchantMappingNotFoundException;
import com.korpay.billpay.exception.webhook.SignatureVerificationFailedException;
import com.korpay.billpay.exception.webhook.WebhookProcessingException;
import com.korpay.billpay.service.settlement.SettlementService;
import com.korpay.billpay.service.transaction.MerchantMappingService;
import com.korpay.billpay.service.transaction.TransactionService;
import com.korpay.billpay.service.webhook.adapter.PgWebhookAdapter;
import com.korpay.billpay.repository.UnmappedTransactionRepository;
import com.korpay.billpay.repository.WebhookIdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessingService {

    private final List<PgWebhookAdapter> adapters;
    private final MerchantMappingService merchantMappingService;
    private final TransactionService transactionService;
    private final WebhookLoggingService webhookLoggingService;
    private final SettlementService settlementService;
    private final TransactionTemplate transactionTemplate;
    private final WebhookIdempotencyKeyRepository webhookIdempotencyKeyRepository;
    private final UnmappedTransactionRepository unmappedTransactionRepository;

    private Map<String, PgWebhookAdapter> adapterMap;

    public WebhookResponse processWebhook(
            String pgCode,
            Long pgConnectionId,
            String webhookSecret,
            String rawBody,
            Map<String, String> headers) {

        log.info("Processing webhook for PG: {}, Connection ID: {}", pgCode, pgConnectionId);

        PgWebhookAdapter adapter = getAdapter(pgCode);
        String signature = extractSignature(headers, pgCode);
        boolean signatureVerified = adapter.verifySignature(rawBody, headers, webhookSecret);

        WebhookLog webhookLog = webhookLoggingService.logWebhookReceived(
                pgConnectionId,
                pgCode,
                rawBody,
                headers,
                signature,
                signatureVerified
        );

        if (!signatureVerified) {
            log.error("Webhook signature verification failed for PG: {}", pgCode);
            webhookLoggingService.updateToFailed(webhookLog.getId(), "Signature verification failed");
            throw new SignatureVerificationFailedException("Invalid webhook signature");
        }

        try {
            webhookLoggingService.updateToProcessing(webhookLog.getId());

            TransactionDto transactionDto = adapter.parse(rawBody, headers);
            log.info("Parsed webhook data. PG TID: {}, Merchant No: {}, Event Type: {}",
                    transactionDto.getPgTid(),
                    transactionDto.getPgMerchantNo(),
                    transactionDto.getEventType());

            // 멱등성 체크
            Optional<WebhookIdempotencyKey> existingKey = webhookIdempotencyKeyRepository
                    .findByPgConnectionIdAndPgTid(pgConnectionId, transactionDto.getPgTid());
            if (existingKey.isPresent()) {
                log.info("Duplicate webhook detected (idempotent): pgConnectionId={}, pgTid={}",
                        pgConnectionId, transactionDto.getPgTid());
                webhookLoggingService.updateToIgnored(webhookLog.getId(),
                        "Duplicate webhook: " + transactionDto.getPgTid());
                return WebhookResponse.duplicate(transactionDto.getPgTid());
            }

            // 멱등성 키 등록
            WebhookIdempotencyKey idempotencyKey = WebhookIdempotencyKey.builder()
                    .pgConnectionId(pgConnectionId)
                    .pgTid(transactionDto.getPgTid())
                    .build();
            webhookIdempotencyKeyRepository.save(idempotencyKey);

            MerchantPgMapping merchantPgMapping;
            try {
                merchantPgMapping = merchantMappingService.findByPgCodeAndPgMerchantNo(
                        pgConnectionId, transactionDto.getPgMerchantNo());
            } catch (MerchantMappingNotFoundException e) {
                log.warn("Merchant mapping not found. Saving unmapped transaction. PG: {}, Merchant No: {}",
                        pgCode, transactionDto.getPgMerchantNo());

                // unmapped_transactions에 저장
                UnmappedTransaction unmapped = UnmappedTransaction.builder()
                        .pgConnectionId(pgConnectionId)
                        .pgTid(transactionDto.getPgTid())
                        .pgMerchantNo(transactionDto.getPgMerchantNo())
                        .rawData(Map.of("rawBody", rawBody))
                        .amount(transactionDto.getAmount())
                        .transactedAt(transactionDto.getTransactedAt() != null ?
                                transactionDto.getTransactedAt() :
                                java.time.OffsetDateTime.now())
                        .build();
                unmappedTransactionRepository.save(unmapped);

                webhookLoggingService.updateToIgnored(webhookLog.getId(),
                        "Unmapped merchant: " + transactionDto.getPgMerchantNo());
                return WebhookResponse.success("Unmapped transaction saved");
            }

            final MerchantPgMapping finalMapping = merchantPgMapping;

            // 트랜잭션 격리 수준 설정 (REPEATABLE_READ)
            transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

            record TransactionResult(Transaction transaction, TransactionEvent event) {}

            TransactionResult result = transactionTemplate.execute(status -> {
                Transaction transaction = transactionService.createOrUpdateFromWebhook(
                        transactionDto, finalMapping);

                TransactionEvent event = transactionService.createTransactionEvent(transaction, transactionDto);

                settlementService.processTransactionEvent(event);

                return new TransactionResult(transaction, event);
            });

            // 멱등성 키 상태 업데이트 (COMPLETED)
            webhookIdempotencyKeyRepository.updateStatus(pgConnectionId, transactionDto.getPgTid(), "COMPLETED");

            webhookLoggingService.updateToProcessed(webhookLog.getId(), result.transaction().getId(), result.event().getId());

            log.info("Successfully processed webhook. Transaction ID: {}, Event Sequence: {}",
                    result.transaction().getTransactionId(), result.event().getEventSequence());

            return WebhookResponse.success(result.transaction().getTransactionId());

        } catch (DuplicateTransactionException e) {
            log.info("Duplicate transaction detected (idempotent): {}", e.getPgTid());
            webhookLoggingService.updateToIgnored(webhookLog.getId(), "Duplicate transaction: " + e.getPgTid());
            return WebhookResponse.duplicate(e.getPgTid());

        } catch (SignatureVerificationFailedException e) {
            webhookLoggingService.updateToFailed(webhookLog.getId(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("Webhook processing failed for PG: {}", pgCode, e);

            // 멱등성 키 상태 업데이트 (FAILED)
            try {
                webhookIdempotencyKeyRepository.updateStatus(pgConnectionId, null, "FAILED");
            } catch (Exception keyUpdateException) {
                log.warn("Failed to update idempotency key status", keyUpdateException);
            }

            webhookLoggingService.updateToFailed(webhookLog.getId(), e.getMessage());
            throw new WebhookProcessingException("Failed to process webhook", e);
        }
    }

    private String extractSignature(Map<String, String> headers, String pgCode) {
        return switch (pgCode.toUpperCase()) {
            case "KORPAY" -> headers.getOrDefault("X-Korpay-Signature", headers.getOrDefault("x-korpay-signature", ""));
            case "NICE" -> headers.getOrDefault("X-Nice-Signature", headers.getOrDefault("x-nice-signature", ""));
            default -> headers.getOrDefault("X-Webhook-Signature", headers.getOrDefault("x-webhook-signature", ""));
        };
    }

    private PgWebhookAdapter getAdapter(String pgCode) {
        if (adapterMap == null) {
            adapterMap = adapters.stream()
                    .collect(Collectors.toMap(PgWebhookAdapter::getPgCode, Function.identity()));
        }

        PgWebhookAdapter adapter = adapterMap.get(pgCode);
        if (adapter == null) {
            throw new WebhookProcessingException("No adapter found for PG: " + pgCode);
        }

        return adapter;
    }
}
