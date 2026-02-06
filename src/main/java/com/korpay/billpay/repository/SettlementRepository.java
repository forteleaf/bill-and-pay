package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.enums.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    interface OrganizationSettlementAggregation {
        UUID getOrganizationId();
        String getOrgCode();
        String getOrgName();
        String getOrgType();
        String getOrgPath();
        Integer getLevel();
        UUID getBusinessEntityId();
        String getRepresentativeName();
        String getMainPhone();
        Long getMerchantCount();
        Long getApprovalAmount();
        Long getApprovalCount();
        Long getCancelAmount();
        Long getCancelCount();
        Long getNetPaymentAmount();
        Long getTotalTransactionCount();
        Long getMerchantFeeAmount();
        Long getOrgFeeAmount();
        BigDecimal getAvgFeeRate();
        Long getCompletedCount();
        Long getPendingCount();
        Long getFailedCount();
    }

    interface MerchantSettlementAggregation {
        UUID getMerchantId();
        String getMerchantCode();
        String getMerchantName();
        java.time.LocalDate getTransactionDate();
        String getBranchName();
        Long getApprovalAmount();
        Long getApprovalCount();
        Long getCancelAmount();
        Long getCancelCount();
        Long getNetPaymentAmount();
        Long getFeeAmount();
        BigDecimal getFeeRate();
    }

    interface HierarchyFeeAggregation {
        String getEntityType();
        String getEntityName();
        String getEntityCode();
        BigDecimal getFeeRate();
        Long getFeeAmount();
        Long getNetAmount();
    }

    @Query(value = """
        SELECT s.* FROM settlements s
        JOIN merchants m ON s.merchant_id = m.id
        WHERE s.settlement_batch_id IS NULL
          AND CAST(s.status AS TEXT) = 'PENDING'
          AND s.created_at >= CAST(:start AS TIMESTAMP WITH TIME ZONE)
          AND s.created_at < CAST(:end AS TIMESTAMP WITH TIME ZONE)
          AND CAST(m.settlement_cycle AS TEXT) = :cycle
        """, nativeQuery = true)
    List<Settlement> findUnbatchedSettlementsByCycle(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            @Param("cycle") String cycle);

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
        AND CAST(s.status AS TEXT) = CAST(:status AS TEXT)
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
        AND (CAST(:startDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE))
        AND (CAST(:endDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP WITH TIME ZONE))
        ORDER BY s.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM settlements s
        WHERE (:userPath = '' OR s.entity_path <@ CAST(:userPath AS ltree))
        AND (:entityType IS NULL OR CAST(s.entity_type AS TEXT) = :entityType)
        AND (:status IS NULL OR CAST(s.status AS TEXT) = :status)
        AND (CAST(:startDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE))
        AND (CAST(:endDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP WITH TIME ZONE))
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
        AND (CAST(:startDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE))
        AND (CAST(:endDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP WITH TIME ZONE))
        """, nativeQuery = true)
    List<Settlement> findAccessibleSettlementsForSummary(
            @Param("userPath") String userPath,
            @Param("entityType") String entityType,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = """
        SELECT s.* FROM settlements s
        WHERE (:userPath = '' OR s.entity_path <@ CAST(:userPath AS ltree))
        AND s.created_at >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE)
        AND s.created_at < CAST(:endDate AS TIMESTAMP WITH TIME ZONE)
        ORDER BY s.created_at DESC
        """, nativeQuery = true)
    List<Settlement> findAccessibleSettlementsInDateRange(
            @Param("userPath") String userPath,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = """
        SELECT 
            o.id as organizationId,
            o.org_code as orgCode,
            o.name as orgName,
            CAST(o.org_type AS TEXT) as orgType,
            CAST(o.path AS TEXT) as orgPath,
            o.level as level,
            be.id as businessEntityId,
            be.representative_name as representativeName,
            be.main_phone as mainPhone,
            COALESCE(mc.merchant_count, 0) as merchantCount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END), 0) as approvalAmount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN 1 ELSE 0 END), 0) as approvalCount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN ABS(s.amount) ELSE 0 END), 0) as cancelAmount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN 1 ELSE 0 END), 0) as cancelCount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE -ABS(s.amount) END), 0) as netPaymentAmount,
            COUNT(s.id) as totalTransactionCount,
            COALESCE(SUM(CASE WHEN s.entity_type = 'VENDOR' THEN s.fee_amount ELSE 0 END), 0) as merchantFeeAmount,
            COALESCE(SUM(s.net_amount), 0) as orgFeeAmount,
            COALESCE(AVG(s.fee_rate), 0) as avgFeeRate,
            COALESCE(SUM(CASE WHEN CAST(s.status AS TEXT) = 'COMPLETED' THEN 1 ELSE 0 END), 0) as completedCount,
            COALESCE(SUM(CASE WHEN CAST(s.status AS TEXT) = 'PENDING' THEN 1 ELSE 0 END), 0) as pendingCount,
            COALESCE(SUM(CASE WHEN CAST(s.status AS TEXT) = 'FAILED' THEN 1 ELSE 0 END), 0) as failedCount
        FROM organizations o
        LEFT JOIN business_entities be ON o.business_entity_id = be.id
        LEFT JOIN (
            SELECT org_id, COUNT(*) as merchant_count 
            FROM merchants 
            WHERE deleted_at IS NULL 
            GROUP BY org_id
        ) mc ON mc.org_id = o.id
        LEFT JOIN settlements s ON s.entity_id = o.id
            AND (CAST(:startDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE))
            AND (CAST(:endDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP WITH TIME ZONE))
        WHERE (:userPath = '' OR o.path <@ CAST(:userPath AS ltree))
            AND (:orgType IS NULL OR CAST(o.org_type AS TEXT) = :orgType)
            AND (:search IS NULL OR o.name ILIKE '%' || :search || '%' OR o.org_code ILIKE '%' || :search || '%')
            AND o.deleted_at IS NULL
        GROUP BY o.id, o.org_code, o.name, o.org_type, o.path, o.level, be.id, be.representative_name, be.main_phone, mc.merchant_count
        ORDER BY o.path
        """, nativeQuery = true)
    List<OrganizationSettlementAggregation> aggregateSettlementsByOrganization(
            @Param("userPath") String userPath,
            @Param("orgType") String orgType,
            @Param("search") String search,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = """
        SELECT 
            m.id as merchantId,
            m.merchant_code as merchantCode,
            m.name as merchantName,
            DATE(s.created_at) as transactionDate,
            o.name as branchName,
            COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END), 0) as approvalAmount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN 1 ELSE 0 END), 0) as approvalCount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN ABS(s.amount) ELSE 0 END), 0) as cancelAmount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'DEBIT' THEN 1 ELSE 0 END), 0) as cancelCount,
            COALESCE(SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE -ABS(s.amount) END), 0) as netPaymentAmount,
            COALESCE(SUM(s.fee_amount), 0) as feeAmount,
            COALESCE(AVG(s.fee_rate), 0) as feeRate
        FROM settlements s
        JOIN merchants m ON s.merchant_id = m.id
        JOIN organizations o ON m.org_id = o.id
        WHERE s.org_path <@ CAST(:orgPath AS ltree)
            AND (CAST(:startDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE))
            AND (CAST(:endDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP WITH TIME ZONE))
        GROUP BY m.id, m.merchant_code, m.name, DATE(s.created_at), o.name
        ORDER BY DATE(s.created_at) DESC, m.name
        """, nativeQuery = true)
    List<MerchantSettlementAggregation> aggregateMerchantSettlements(
            @Param("orgPath") String orgPath,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = """
        SELECT 
            CAST(s.entity_type AS TEXT) as entityType,
            o.name as entityName,
            o.org_code as entityCode,
            COALESCE(AVG(s.fee_rate), 0) as feeRate,
            COALESCE(SUM(s.fee_amount), 0) as feeAmount,
            COALESCE(SUM(s.net_amount), 0) as netAmount
        FROM settlements s
        JOIN organizations o ON s.entity_id = o.id
        WHERE s.org_path <@ CAST(:orgPath AS ltree)
            AND (CAST(:startDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE))
            AND (CAST(:endDate AS TIMESTAMP WITH TIME ZONE) IS NULL OR s.created_at <= CAST(:endDate AS TIMESTAMP WITH TIME ZONE))
        GROUP BY s.entity_type, o.name, o.org_code
        ORDER BY 
            CASE CAST(s.entity_type AS TEXT)
                WHEN 'DISTRIBUTOR' THEN 1
                WHEN 'AGENCY' THEN 2
                WHEN 'DEALER' THEN 3
                WHEN 'SELLER' THEN 4
                WHEN 'VENDOR' THEN 5
            END
        """, nativeQuery = true)
    List<HierarchyFeeAggregation> aggregateHierarchyFees(
            @Param("orgPath") String orgPath,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}
