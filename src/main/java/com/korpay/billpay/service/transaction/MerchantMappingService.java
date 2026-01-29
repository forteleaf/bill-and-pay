package com.korpay.billpay.service.transaction;

import com.korpay.billpay.domain.entity.MerchantPgMapping;
import com.korpay.billpay.domain.enums.MerchantPgMappingStatus;
import com.korpay.billpay.exception.webhook.MerchantMappingNotFoundException;
import com.korpay.billpay.repository.MerchantPgMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantMappingService {

    private final MerchantPgMappingRepository merchantPgMappingRepository;

    @Cacheable(value = "merchantPgMappings", key = "#pgConnectionId + '_' + #pgMerchantNo")
    public MerchantPgMapping findByPgCodeAndPgMerchantNo(UUID pgConnectionId, String pgMerchantNo) {
        log.debug("Finding merchant mapping for PG connection: {}, merchant no: {}", pgConnectionId, pgMerchantNo);

        return merchantPgMappingRepository.findByMidAndPgConnectionId(pgMerchantNo, pgConnectionId)
                .filter(mapping -> mapping.getStatus() == MerchantPgMappingStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("Merchant mapping not found for PG connection: {}, merchant no: {}", 
                            pgConnectionId, pgMerchantNo);
                    return new MerchantMappingNotFoundException(pgConnectionId.toString(), pgMerchantNo);
                });
    }
}
