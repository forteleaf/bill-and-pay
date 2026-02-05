package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.WebhookLog;
import com.korpay.billpay.domain.enums.WebhookLogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, UUID> {

    Page<WebhookLog> findByPgConnectionIdOrderByReceivedAtDesc(Long pgConnectionId, Pageable pageable);

    Page<WebhookLog> findByStatusOrderByReceivedAtDesc(WebhookLogStatus status, Pageable pageable);

    @Query("SELECT w FROM WebhookLog w WHERE w.status = :status AND w.retryCount < :maxRetries ORDER BY w.receivedAt ASC")
    List<WebhookLog> findRetryableWebhooks(
            @Param("status") WebhookLogStatus status,
            @Param("maxRetries") int maxRetries
    );

    List<WebhookLog> findByTransactionIdOrderByReceivedAtDesc(UUID transactionId);

    @Query("SELECT w FROM WebhookLog w WHERE w.receivedAt BETWEEN :startDate AND :endDate ORDER BY w.receivedAt DESC")
    Page<WebhookLog> findByDateRange(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable
    );

    long countByStatus(WebhookLogStatus status);

    long countByPgConnectionIdAndStatus(Long pgConnectionId, WebhookLogStatus status);

    Optional<WebhookLog> findFirstByPgConnectionIdOrderByReceivedAtDesc(Long pgConnectionId);

    @Modifying
    @Query("UPDATE WebhookLog w SET w.status = :status, w.processedAt = :processedAt WHERE w.id = :id")
    int updateStatus(
            @Param("id") UUID id,
            @Param("status") WebhookLogStatus status,
            @Param("processedAt") OffsetDateTime processedAt
    );

    @Modifying
    @Query("UPDATE WebhookLog w SET w.status = :status, w.processedAt = :processedAt, w.errorMessage = :errorMessage, w.retryCount = w.retryCount + 1 WHERE w.id = :id")
    int updateStatusWithError(
            @Param("id") UUID id,
            @Param("status") WebhookLogStatus status,
            @Param("processedAt") OffsetDateTime processedAt,
            @Param("errorMessage") String errorMessage
    );

    @Query("SELECT w FROM WebhookLog w WHERE w.status = :status AND w.receivedAt < :before")
    List<WebhookLog> findOldWebhooksByStatus(
            @Param("status") WebhookLogStatus status,
            @Param("before") OffsetDateTime before
    );

    @Modifying
    @Query("DELETE FROM WebhookLog w WHERE w.status IN (:statuses) AND w.receivedAt < :before")
    int deleteOldWebhooks(
            @Param("statuses") List<WebhookLogStatus> statuses,
            @Param("before") OffsetDateTime before
    );
}
