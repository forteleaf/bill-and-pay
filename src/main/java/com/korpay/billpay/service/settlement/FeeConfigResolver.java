package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.exception.settlement.FeeConfigNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeConfigResolver {

    private static final String FEE_CONFIG_KEY = "feeRates";
    private static final String DEFAULT_KEY = "default";

    public BigDecimal resolveMerchantFeeRate(Merchant merchant, String paymentMethodCode) {
        Map<String, Object> config = merchant.getConfig();
        if (config == null || !config.containsKey(FEE_CONFIG_KEY)) {
            throw new FeeConfigNotFoundException(
                    merchant.getId(),
                    "MERCHANT",
                    paymentMethodCode
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> feeRates = (Map<String, Object>) config.get(FEE_CONFIG_KEY);

        Object rateValue = feeRates.getOrDefault(paymentMethodCode, feeRates.get(DEFAULT_KEY));
        if (rateValue == null) {
            throw new FeeConfigNotFoundException(
                    merchant.getId(),
                    "MERCHANT",
                    paymentMethodCode
            );
        }

        return convertToFeeRate(rateValue);
    }

    public BigDecimal resolveOrganizationFeeRate(Organization organization, String paymentMethodCode) {
        Map<String, Object> config = organization.getConfig();
        if (config == null || !config.containsKey(FEE_CONFIG_KEY)) {
            throw new FeeConfigNotFoundException(
                    organization.getId(),
                    organization.getOrgType().name(),
                    paymentMethodCode
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> feeRates = (Map<String, Object>) config.get(FEE_CONFIG_KEY);

        Object rateValue = feeRates.getOrDefault(paymentMethodCode, feeRates.get(DEFAULT_KEY));
        if (rateValue == null) {
            throw new FeeConfigNotFoundException(
                    organization.getId(),
                    organization.getOrgType().name(),
                    paymentMethodCode
            );
        }

        return convertToFeeRate(rateValue);
    }

    private BigDecimal convertToFeeRate(Object rateValue) {
        if (rateValue instanceof Number) {
            return BigDecimal.valueOf(((Number) rateValue).doubleValue());
        } else if (rateValue instanceof String) {
            return new BigDecimal((String) rateValue);
        } else {
            throw new IllegalArgumentException("Invalid fee rate format: " + rateValue);
        }
    }
}
