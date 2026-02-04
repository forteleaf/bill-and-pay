package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, TransactionEvent.TransactionEventId> {

    List<TransactionEvent> findByTransactionIdOrderByEventSequenceAsc(UUID transactionId);

    java.util.Optional<TransactionEvent> findTopByTransactionIdOrderByEventSequenceDesc(UUID transactionId);

    List<TransactionEvent> findByMerchantIdAndCreatedAtBetween(
            UUID merchantId,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    );

    @Query(value = "SELECT * FROM transaction_events WHERE org_path <@ CAST(:path AS public.ltree) AND created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<TransactionEvent> findByOrgPathDescendantsAndDateRange(
            @Param("path") String path,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    List<TransactionEvent> findByEventTypeAndOccurredAtBetween(
            EventType eventType,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    );
}
