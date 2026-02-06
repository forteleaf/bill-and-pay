package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.SettlementBatch;
import com.korpay.billpay.domain.enums.SettlementBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, UUID> {

    Optional<SettlementBatch> findByBatchNumber(String batchNumber);

    List<SettlementBatch> findBySettlementDate(LocalDate settlementDate);

    List<SettlementBatch> findByStatus(SettlementBatchStatus status);

    List<SettlementBatch> findBySettlementDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsBySettlementDateAndBatchNumberContaining(LocalDate date, String cyclePrefix);
}
