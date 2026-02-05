package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.FeeConfiguration;
import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.PaymentMethod;
import com.korpay.billpay.domain.enums.FeeConfigStatus;
import com.korpay.billpay.exception.settlement.FeeConfigNotFoundException;
import com.korpay.billpay.repository.FeeConfigurationRepository;
import com.korpay.billpay.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeConfigResolver {

    private final FeeConfigurationRepository feeConfigurationRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public BigDecimal resolveMerchantFeeRate(Merchant merchant, String paymentMethodCode) {
        Organization vendorOrg = merchant.getOrganization();
        
        PaymentMethod paymentMethod = paymentMethodRepository.findByMethodCode(paymentMethodCode)
                .orElseThrow(() -> new FeeConfigNotFoundException(
                        merchant.getId(),
                        "MERCHANT",
                        paymentMethodCode
                ));

        List<FeeConfiguration> configs = feeConfigurationRepository.findActiveByEntityAndPaymentMethod(
                vendorOrg.getId(),
                vendorOrg.getOrgType(),
                paymentMethod.getId(),
                FeeConfigStatus.ACTIVE,
                OffsetDateTime.now()
        );

        if (configs.isEmpty()) {
            log.warn("No fee configuration found for merchant {} (vendor org: {}, type: {}), paymentMethod: {}",
                    merchant.getId(), vendorOrg.getId(), vendorOrg.getOrgType(), paymentMethodCode);
            throw new FeeConfigNotFoundException(
                    vendorOrg.getId(),
                    vendorOrg.getOrgType().name(),
                    paymentMethodCode
            );
        }

        FeeConfiguration config = configs.getFirst();
        log.debug("Resolved merchant fee rate: {} for merchant {} using vendor org {}", 
                config.getFeeRate(), merchant.getId(), vendorOrg.getId());
        
        return config.getFeeRate();
    }

    public BigDecimal resolveOrganizationFeeRate(Organization organization, String paymentMethodCode) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByMethodCode(paymentMethodCode)
                .orElseThrow(() -> new FeeConfigNotFoundException(
                        organization.getId(),
                        organization.getOrgType().name(),
                        paymentMethodCode
                ));

        List<FeeConfiguration> configs = feeConfigurationRepository.findActiveByEntityAndPaymentMethod(
                organization.getId(),
                organization.getOrgType(),
                paymentMethod.getId(),
                FeeConfigStatus.ACTIVE,
                OffsetDateTime.now()
        );

        if (configs.isEmpty()) {
            log.warn("No fee configuration found for organization {} (type: {}), paymentMethod: {}",
                    organization.getId(), organization.getOrgType(), paymentMethodCode);
            throw new FeeConfigNotFoundException(
                    organization.getId(),
                    organization.getOrgType().name(),
                    paymentMethodCode
            );
        }

        FeeConfiguration config = configs.getFirst();
        log.debug("Resolved organization fee rate: {} for org {} (type: {})", 
                config.getFeeRate(), organization.getId(), organization.getOrgType());
        
        return config.getFeeRate();
    }
}
