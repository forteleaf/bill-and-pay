package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Find accessible settlements using ltree descendant operator with filters and pagination.
     * MASTER_ADMIN: Pass empty string or root path to see all.
     * Regular users: Pass their orgPath to see only descendants.
     */
    @Query(value = """
        SELECT s.* FROM settlements s
        WHERE (:userPath = '' OR s.entity_path <@ CAST(:userPath AS ltree))
        AND (:entityType IS NULL OR CAST(s.entity_type AS TEXT) = :entityType)
        AND (:status IS NULL OR CAST(s.status AS TEXT) = :status)
        AND (:startDate IS NULL OR s.created_at >= :startDate)
        AND (:endDate IS NULL OR s.created_at <= :endDate)
        ORDER BY s.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM settlements s
        WHERE (:userPath = '' OR s.entity_path <@ CAST(:userPath AS ltree))
        AND (:entityType IS NULL OR CAST(s.entity_type AS TEXT) = :entityType)
        AND (:status IS NULL OR CAST(s.status AS TEXT) = :status)
        AND (:startDate IS NULL OR s.created_at >= :startDate)
        AND (:endDate IS NULL OR s.created_at <= :endDate)
        """,
        nativeQuery = true)
    Page<Settlement> findAccessibleSettlements(
            @Param("userPath") String userPath,
            @Param("entityType") String entityType,
            @Param("status") String status,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable
    );

    /**
     * Find all accessible settlements for aggregation (no pagination).
     * Used for summary calculations.
     */
    @Query(value = """
        SELECT s.* FROM settlements s
        WHERE (:userPath = '' OR s.entity_path <@ CAST(:userPath AS ltree))
        AND (:entityType IS NULL OR CAST(s.entity_type AS TEXT) = :entityType)
        AND (:startDate IS NULL OR s.created_at >= :startDate)
        AND (:endDate IS NULL OR s.created_at <= :endDate)
        """, nativeQuery = true)
    List<Settlement> findAccessibleSettlementsForSummary(
            @Param("userPath") String userPath,
            @Param("entityType") String entityType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Find accessible settlements within a specific date range.
     * Used for daily batch reports.
     */
    @Query(value = """
        SELECT s.* FROM settlements s
        WHERE (:userPath = '' OR s.entity_path <@ CAST(:userPath AS ltree))
        AND s.created_at >= :startDate
        AND s.created_at < :endDate
        ORDER BY s.created_at DESC
        """, nativeQuery = true)
    List<Settlement> findAccessibleSettlementsInDateRange(
            @Param("userPath") String userPath,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
