package com.korpay.billpay.service.transaction;

import com.korpay.billpay.domain.entity.Transaction;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.TransactionStatus;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.repository.TransactionEventRepository;
import com.korpay.billpay.repository.TransactionRepository;
import com.korpay.billpay.service.auth.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final AccessControlService accessControlService;

    public Page<Transaction> findAccessibleTransactions(
            User user,
            UUID merchantId,
            TransactionStatus status,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Pageable pageable) {
        
        List<Transaction> allTransactions;
        
        if (merchantId != null) {
            allTransactions = transactionRepository.findByMerchantId(merchantId, Pageable.unpaged()).getContent();
        } else if (status != null && startDate != null && endDate != null) {
            allTransactions = transactionRepository.findByStatusAndCreatedAtBetween(status, startDate, endDate);
        } else {
            allTransactions = transactionRepository.findAll();
        }
        
        List<Transaction> accessibleTransactions = allTransactions.stream()
                .filter(txn -> accessControlService.hasAccessToOrganization(user, txn.getOrgPath()))
                .collect(Collectors.toList());
        
        if (status != null) {
            accessibleTransactions = accessibleTransactions.stream()
                    .filter(txn -> txn.getStatus() == status)
                    .collect(Collectors.toList());
        }
        
        if (startDate != null) {
            accessibleTransactions = accessibleTransactions.stream()
                    .filter(txn -> txn.getCreatedAt().isAfter(startDate) || txn.getCreatedAt().isEqual(startDate))
                    .collect(Collectors.toList());
        }
        
        if (endDate != null) {
            accessibleTransactions = accessibleTransactions.stream()
                    .filter(txn -> txn.getCreatedAt().isBefore(endDate) || txn.getCreatedAt().isEqual(endDate))
                    .collect(Collectors.toList());
        }
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), accessibleTransactions.size());
        
        List<Transaction> pageContent = accessibleTransactions.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, accessibleTransactions.size());
    }

    public Transaction findById(UUID id, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
        
        accessControlService.validateOrganizationAccess(user, transaction.getOrgPath());
        
        return transaction;
    }

    public List<TransactionEvent> findEventsByTransactionId(UUID transactionId, User user) {
        Transaction transaction = findById(transactionId, user);
        
        return transactionEventRepository.findByTransactionIdOrderByEventSequenceAsc(transaction.getId());
    }
}
