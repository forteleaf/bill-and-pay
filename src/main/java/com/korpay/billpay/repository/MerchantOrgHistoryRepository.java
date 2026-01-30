package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.MerchantOrgHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantOrgHistoryRepository extends JpaRepository<MerchantOrgHistory, UUID> {

    Page<MerchantOrgHistory> findByMerchantIdOrderByMovedAtDesc(UUID merchantId, Pageable pageable);

    List<MerchantOrgHistory> findByMerchantIdOrderByMovedAtDesc(UUID merchantId);

    long countByMerchantId(UUID merchantId);
}
