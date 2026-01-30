package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    List<Settlement> findByTransactionEventId(UUID transactionEventId);

    List<Settlement> findByEntityIdAndStatus(UUID entityId, SettlementStatus status);

    @Query(value = "SELECT * FROM settlements WHERE entity_path <@ CAST(:path AS ltree)", nativeQuery = true)
    List<Settlement> findByEntityPathDescendants(@Param("path") String path);

    List<Settlement> findBySettlementBatchId(UUID settlementBatchId);

    @Query(value = "SELECT * FROM settlements WHERE entity_id = :entityId AND created_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<Settlement> findByEntityIdAndDateRange(
            @Param("entityId") UUID entityId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = "SELECT SUM(net_amount) FROM settlements WHERE entity_id = :entityId AND status = :status", nativeQuery = true)
    Long sumNetAmountByEntityIdAndStatus(
            @Param("entityId") UUID entityId,
            @Param("status") String status
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM settlements s
        WHERE s.entity_path <@ CAST(:entityPath AS ltree)
        AND s.status = CAST(:status AS settlement_status)
        """, nativeQuery = true)
    Long countByEntityPathStartingWithAndStatus(
            @Param("entityPath") String entityPath,
            @Param("status") SettlementStatus status
    );
}
