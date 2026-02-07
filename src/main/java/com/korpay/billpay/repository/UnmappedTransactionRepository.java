package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.UnmappedTransaction;
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
public interface UnmappedTransactionRepository extends JpaRepository<UnmappedTransaction, UUID> {

    Optional<UnmappedTransaction> findByPgConnectionIdAndPgTid(Long pgConnectionId, String pgTid);

    Page<UnmappedTransaction> findByStatus(String status, Pageable pageable);

    List<UnmappedTransaction> findByPgConnectionIdAndPgMerchantNo(Long pgConnectionId, String pgMerchantNo);

    @Query("SELECT u FROM UnmappedTransaction u WHERE u.status = :status AND u.receivedAt BETWEEN :startDate AND :endDate")
    List<UnmappedTransaction> findByStatusAndReceivedAtBetween(
            @Param("status") String status,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );

    Long countByStatus(String status);
}
