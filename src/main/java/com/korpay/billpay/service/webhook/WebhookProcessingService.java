package com.korpay.billpay.service.webhook;

import com.korpay.billpay.domain.entity.MerchantPgMapping;
import com.korpay.billpay.domain.entity.Transaction;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.dto.webhook.TransactionDto;
import com.korpay.billpay.dto.webhook.WebhookResponse;
import com.korpay.billpay.exception.webhook.DuplicateTransactionException;
import com.korpay.billpay.exception.webhook.MerchantMappingNotFoundException;
import com.korpay.billpay.exception.webhook.SignatureVerificationFailedException;
import com.korpay.billpay.exception.webhook.WebhookProcessingException;
import com.korpay.billpay.service.transaction.MerchantMappingService;
import com.korpay.billpay.service.transaction.TransactionService;
import com.korpay.billpay.service.webhook.adapter.PgWebhookAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessingService {

    private final List<PgWebhookAdapter> adapters;
    private final MerchantMappingService merchantMappingService;
    private final TransactionService transactionService;

    private Map<String, PgWebhookAdapter> adapterMap;

    @Transactional
    public WebhookResponse processWebhook(
            String pgCode,
            UUID pgConnectionId,
            String webhookSecret,
            String rawBody,
            Map<String, String> headers) {

        log.info("Processing webhook for PG: {}, Connection ID: {}", pgCode, pgConnectionId);

        try {
            PgWebhookAdapter adapter = getAdapter(pgCode);

            if (!adapter.verifySignature(rawBody, headers, webhookSecret)) {
                log.error("Webhook signature verification failed for PG: {}", pgCode);
                throw new SignatureVerificationFailedException("Invalid webhook signature");
            }

            TransactionDto transactionDto = adapter.parse(rawBody, headers);
            log.info("Parsed webhook data. PG TID: {}, Merchant No: {}, Event Type: {}",
                    transactionDto.getPgTid(),
                    transactionDto.getPgMerchantNo(),
                    transactionDto.getEventType());

            MerchantPgMapping merchantPgMapping;
            try {
                merchantPgMapping = merchantMappingService.findByPgCodeAndPgMerchantNo(
                        pgConnectionId, transactionDto.getPgMerchantNo());
            } catch (MerchantMappingNotFoundException e) {
                log.warn("Merchant mapping not found. Saving to unmapped transactions. PG: {}, Merchant No: {}",
                        pgCode, transactionDto.getPgMerchantNo());
                return WebhookResponse.success("Unmapped transaction saved");
            }

            Transaction transaction = transactionService.createOrUpdateFromWebhook(
                    transactionDto, merchantPgMapping);

            TransactionEvent event = transactionService.createTransactionEvent(transaction, transactionDto);

            log.info("Successfully processed webhook. Transaction ID: {}, Event Sequence: {}",
                    transaction.getTransactionId(), event.getEventSequence());

            return WebhookResponse.success(transaction.getTransactionId());

        } catch (DuplicateTransactionException e) {
            log.info("Duplicate transaction detected (idempotent): {}", e.getPgTid());
            return WebhookResponse.duplicate(e.getPgTid());

        } catch (SignatureVerificationFailedException e) {
            log.error("Signature verification failed", e);
            throw e;

        } catch (Exception e) {
            log.error("Webhook processing failed for PG: {}", pgCode, e);
            throw new WebhookProcessingException("Failed to process webhook", e);
        }
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
