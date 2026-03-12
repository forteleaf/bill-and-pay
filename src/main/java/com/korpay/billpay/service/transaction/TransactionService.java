package com.korpay.billpay.service.transaction;

import com.korpay.billpay.domain.entity.*;
import com.korpay.billpay.domain.enums.EventType;
import com.korpay.billpay.domain.enums.TransactionStatus;
import com.korpay.billpay.dto.webhook.TransactionDto;
import com.korpay.billpay.exception.webhook.DuplicateTransactionException;
import com.korpay.billpay.exception.webhook.WebhookProcessingException;
import com.korpay.billpay.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final CardCompanyRepository cardCompanyRepository;

    @Transactional
    public Transaction createOrUpdateFromWebhook(TransactionDto dto, MerchantPgMapping merchantPgMapping) {
        log.info("Creating or updating transaction from webhook. PG TID: {}, Event Type: {}",
                dto.getPgTid(), dto.getEventType());

        if (dto.getEventType() == EventType.APPROVAL) {
            // 승인: pgTid로 중복 체크
            Optional<Transaction> existingTransaction = transactionRepository.findByPgConnectionIdAndPgTransactionId(
                    merchantPgMapping.getPgConnectionId(), dto.getPgTid());

            if (existingTransaction.isPresent()) {
                log.warn("Duplicate approval transaction detected: pgConnectionId={}, pgTid={}",
                        merchantPgMapping.getPgConnectionId(), dto.getPgTid());
                throw new DuplicateTransactionException(dto.getPgTid());
            }
            return createTransaction(dto, merchantPgMapping);
        } else {
            // 취소/부분취소: pgOtid(원본 TID)로 원본 거래 조회
            String originalTid = dto.getPgOtid();
            if (originalTid == null || originalTid.isEmpty()) {
                log.error("Cancel event missing pgOtid: pgTid={}", dto.getPgTid());
                throw new WebhookProcessingException("Cancel event requires pgOtid (original transaction ID)");
            }

            Optional<Transaction> originalTransaction = transactionRepository.findByPgConnectionIdAndPgTransactionId(
                    merchantPgMapping.getPgConnectionId(), originalTid);

            if (originalTransaction.isEmpty()) {
                log.error("Original transaction not found for cancel: pgConnectionId={}, pgOtid={}",
                        merchantPgMapping.getPgConnectionId(), originalTid);
                throw new WebhookProcessingException("Original transaction not found for cancel event: " + originalTid);
            }
            return updateTransaction(originalTransaction.get(), dto);
        }
    }

    @Transactional
    public TransactionEvent createTransactionEvent(Transaction transaction, TransactionDto dto) {
        log.debug("Creating transaction event for transaction: {}, event type: {}",
                transaction.getTransactionId(), dto.getEventType());

        Integer nextSequence = calculateNextEventSequence(transaction);

        TransactionEvent event = TransactionEvent.builder()
                .createdAt(OffsetDateTime.now())
                .eventType(dto.getEventType())
                .eventSequence(nextSequence)
                .transactionId(transaction.getId())
                .merchantId(transaction.getMerchant().getId())
                .merchantPgMappingId(transaction.getMerchantPgMapping().getId())
                .pgConnectionId(transaction.getPgConnectionId())
                .orgPath(transaction.getOrgPath())
                .paymentMethodId(transaction.getPaymentMethod().getId())
                .cardCompanyId(transaction.getCardCompany() != null ? transaction.getCardCompany().getId() : null)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .previousStatus(determinePreviousStatus(transaction, dto))
                .newStatus(determineNewStatus(dto))
                .pgTransactionId(dto.getPgTid())
                .approvalNumber(dto.getApprovalNo())
                .catId(dto.getTerminalId())
                .metadata(dto.getMetadata())
                .occurredAt(dto.getTransactedAt() != null ? dto.getTransactedAt() : OffsetDateTime.now())
                .build();

        return transactionEventRepository.save(event);
    }

    private Transaction createTransaction(TransactionDto dto, MerchantPgMapping merchantPgMapping) {
        Merchant merchant = merchantPgMapping.getMerchant();

        PaymentMethod paymentMethod = paymentMethodRepository.findByMethodCode(dto.getPaymentMethod())
                .orElseThrow(() -> new WebhookProcessingException("Payment method not found: " + dto.getPaymentMethod()));

        CardCompany cardCompany = null;
        if (dto.getIssuerCode() != null) {
            cardCompany = cardCompanyRepository.findByCompanyCode(dto.getIssuerCode()).orElse(null);
        }

        String transactionId = generateTransactionId(dto);

        Transaction transaction = Transaction.builder()
                .transactionId(transactionId)
                .merchant(merchant)
                .merchantPgMapping(merchantPgMapping)
                .pgConnectionId(merchantPgMapping.getPgConnectionId())
                .orgPath(merchant.getOrgPath())
                .paymentMethod(paymentMethod)
                .cardCompany(cardCompany)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .status(TransactionStatus.APPROVED)
                .pgTransactionId(dto.getPgTid())
                .approvalNumber(dto.getApprovalNo())
                .approvedAt(dto.getTransactedAt())
                .catId(dto.getTerminalId())
                .metadata(dto.getMetadata())
                .build();

        transaction = transactionRepository.saveAndFlush(transaction);
        log.info("Created new transaction: {}", transaction.getTransactionId());

        return transaction;
    }

    private Transaction updateTransaction(Transaction transaction, TransactionDto dto) {
        log.info("Updating transaction: {}, event type: {}", transaction.getTransactionId(), dto.getEventType());

        if (dto.getEventType() == EventType.CANCEL) {
            transaction.setStatus(TransactionStatus.CANCELLED);
            transaction.setCancelledAt(dto.getCanceledAt());
            // amount는 원래 금액을 유지 (DB 제약조건 amount > 0)
        } else if (dto.getEventType() == EventType.PARTIAL_CANCEL) {
            transaction.setStatus(TransactionStatus.PARTIAL_CANCELLED);
            transaction.setCancelledAt(dto.getCanceledAt());
            transaction.setAmount(dto.getRemainAmount());
        }

        transaction = transactionRepository.save(transaction);
        log.info("Updated transaction status to: {}", transaction.getStatus());

        return transaction;
    }

    private Integer calculateNextEventSequence(Transaction transaction) {
        return transactionEventRepository
                .findTopByTransactionIdOrderByEventSequenceDesc(transaction.getId())
                .map(event -> event.getEventSequence() + 1)
                .orElse(1);
    }

    private String determinePreviousStatus(Transaction transaction, TransactionDto dto) {
        if (dto.getEventType() == EventType.APPROVAL) {
            return null;
        }
        return transaction.getStatus().name();
    }

    private String determineNewStatus(TransactionDto dto) {
        return switch (dto.getEventType()) {
            case APPROVAL -> TransactionStatus.APPROVED.name();
            case CANCEL -> TransactionStatus.CANCELLED.name();
            case PARTIAL_CANCEL -> TransactionStatus.PARTIAL_CANCELLED.name();
            default -> TransactionStatus.APPROVED.name();
        };
    }

    private String generateTransactionId(TransactionDto dto) {
        return "TXN-" + dto.getPgTid();
    }
}
