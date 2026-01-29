package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.MerchantPgMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantPgMappingRepository extends JpaRepository<MerchantPgMapping, UUID> {

    List<MerchantPgMapping> findByMerchantId(UUID merchantId);

    Optional<MerchantPgMapping> findByMidAndPgConnectionId(String mid, UUID pgConnectionId);

    List<MerchantPgMapping> findByPgConnectionId(UUID pgConnectionId);

    Optional<MerchantPgMapping> findByCatId(String catId);
}
