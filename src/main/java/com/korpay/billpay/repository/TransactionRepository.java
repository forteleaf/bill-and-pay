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

    @Query(value = "SELECT * FROM transactions WHERE merchant_path <@ CAST(:path AS ltree)", nativeQuery = true)
    List<Transaction> findByMerchantPathDescendants(@Param("path") String path);

    @Query(value = "SELECT * FROM transactions WHERE org_path <@ CAST(:path AS ltree)", nativeQuery = true)
    List<Transaction> findByOrgPathDescendants(@Param("path") String path);

    List<Transaction> findByStatusAndCreatedAtBetween(
            TransactionStatus status,
            OffsetDateTime startDate,
            OffsetDateTime endDate
    );

    Optional<Transaction> findByPgTransactionId(String pgTransactionId);

    Optional<Transaction> findByCatIdAndTid(String catId, String tid);
}
