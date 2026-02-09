package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.FeeConfigHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FeeConfigHistoryRepository extends JpaRepository<FeeConfigHistory, UUID> {

    List<FeeConfigHistory> findByFeeConfigurationIdOrderByChangedAtDesc(UUID feeConfigurationId);

    List<FeeConfigHistory> findByMerchantIdOrderByChangedAtDesc(UUID merchantId);
}
