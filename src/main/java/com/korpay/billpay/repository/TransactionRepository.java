package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Transaction;
import com.korpay.billpay.domain.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionId(String transactionId);

    Page<Transaction> findByMerchantId(UUID merchantId, Pageable pageable);

    @Query("SELECT t FROM Transaction t JOIN FETCH t.merchant JOIN FETCH t.paymentMethod LEFT JOIN FETCH t.cardCompany")
    List<Transaction> findAllWithMerchant();

    @Query(value = "SELECT * FROM transactions WHERE merchant_path <@ CAST(:path AS public.ltree)", nativeQuery = true)
    List<Transaction> findByMerchantPathDescendants(@Param("path") String path);

    @Query(value = "SELECT * FROM transactions WHERE org_path <@ CAST(:path AS public.ltree)", nativeQuery = true)
    List<Transaction> findByOrgPathDescendants(@Param("path") String path);

    List<Transaction> findByStatusAndCreatedAtBetween(
            TransactionStatus status,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    );

    Optional<Transaction> findByPgTransactionId(String pgTransactionId);

    Optional<Transaction> findByPgConnectionIdAndPgTransactionId(Long pgConnectionId, String pgTransactionId);

    @Query(value = """
        SELECT COALESCE(SUM(t.amount), 0)
        FROM transactions t
        WHERE t.org_path <@ CAST(:orgPath AS public.ltree)
        AND CAST(t.status AS TEXT) = CAST(:status AS TEXT)
        AND t.created_at BETWEEN CAST(:startDate AS TIMESTAMP WITH TIME ZONE) AND CAST(:endDate AS TIMESTAMP WITH TIME ZONE)
        """, nativeQuery = true)
    Long sumAmountByOrgPathAndStatusAndDateRange(
            @Param("orgPath") String orgPath,
            @Param("status") TransactionStatus status,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM transactions t
        WHERE t.org_path <@ CAST(:orgPath AS public.ltree)
        AND t.created_at BETWEEN CAST(:startDate AS TIMESTAMP WITH TIME ZONE) AND CAST(:endDate AS TIMESTAMP WITH TIME ZONE)
        """, nativeQuery = true)
    Long countByOrgPathStartingWithAndCreatedAtBetween(
            @Param("orgPath") String orgPath,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    @Query(value = """
        SELECT 
            m.id as merchantId,
            m.name as merchantName,
            SUM(t.amount) as totalAmount,
            COUNT(t.id) as transactionCount
        FROM transactions t
        INNER JOIN merchants m ON t.merchant_id = m.id
        WHERE t.org_path <@ CAST(:orgPath AS public.ltree)
        AND t.status = 'APPROVED'
        AND t.created_at BETWEEN CAST(:startDate AS TIMESTAMP WITH TIME ZONE) AND CAST(:endDate AS TIMESTAMP WITH TIME ZONE)
        GROUP BY m.id, m.name
        ORDER BY totalAmount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopMerchantsByAmountAndOrgPath(
            @Param("orgPath") String orgPath,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            @Param("limit") int limit
    );
}
