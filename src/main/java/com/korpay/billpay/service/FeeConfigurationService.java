package com.korpay.billpay.service;

import com.korpay.billpay.domain.entity.*;
import com.korpay.billpay.domain.enums.FeeConfigStatus;
import com.korpay.billpay.domain.enums.FeeType;
import com.korpay.billpay.dto.request.FeeConfigurationCreateRequest;
import com.korpay.billpay.dto.request.FeeConfigurationUpdateRequest;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.repository.FeeConfigHistoryRepository;
import com.korpay.billpay.repository.FeeConfigurationRepository;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.PaymentMethodRepository;
import com.korpay.billpay.service.auth.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeeConfigurationService {

    private final MerchantRepository merchantRepository;
    private final FeeConfigurationRepository feeConfigurationRepository;
    private final FeeConfigHistoryRepository feeConfigHistoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final UserContextHolder userContextHolder;

    public List<FeeConfiguration> listByMerchant(UUID merchantId) {
        Merchant merchant = findMerchant(merchantId);
        Organization org = merchant.getOrganization();
        return feeConfigurationRepository.findByEntityIdAndEntityTypeOrderByPriorityAsc(
                org.getId(), org.getOrgType());
    }

    public FeeConfiguration getById(UUID id) {
        return feeConfigurationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FeeConfiguration not found: " + id));
    }

    @Transactional
    public FeeConfiguration create(UUID merchantId, FeeConfigurationCreateRequest req) {
        Merchant merchant = findMerchant(merchantId);
        Organization org = merchant.getOrganization();

        validateFeeRate(req.getFeeType(), req.getFeeRate());

        FeeConfiguration config = FeeConfiguration.builder()
                .entityId(org.getId())
                .entityType(org.getOrgType())
                .entityPath(org.getPath())
                .paymentMethodId(req.getPaymentMethodId())
                .feeType(req.getFeeType())
                .feeRate(req.getFeeRate())
                .fixedFee(req.getFixedFee())
                .minFee(req.getMinFee())
                .maxFee(req.getMaxFee())
                .priority(req.getPriority() != null ? req.getPriority() : 0)
                .validFrom(req.getValidFrom() != null ? req.getValidFrom() : OffsetDateTime.now())
                .validUntil(req.getValidUntil())
                .status(FeeConfigStatus.ACTIVE)
                .build();

        FeeConfiguration saved = feeConfigurationRepository.save(config);

        recordHistory(FeeConfigHistory.builder()
                .feeConfigurationId(saved.getId())
                .merchantId(merchantId)
                .action("CREATE")
                .newFeeRate(saved.getFeeRate())
                .newStatus(saved.getStatus().name())
                .reason(req.getReason())
                .changedBy(getCurrentUsername())
                .build());

        return saved;
    }

    @Transactional
    public FeeConfiguration update(UUID id, FeeConfigurationUpdateRequest req) {
        FeeConfiguration config = getById(id);
        String username = getCurrentUsername();

        if (req.getFeeRate() != null && !req.getFeeRate().equals(config.getFeeRate())) {
            validateFeeRate(
                    req.getFeeType() != null ? req.getFeeType() : config.getFeeType(),
                    req.getFeeRate());

            recordHistory(FeeConfigHistory.builder()
                    .feeConfigurationId(id)
                    .merchantId(findMerchantIdByEntityId(config.getEntityId()))
                    .action("UPDATE")
                    .fieldName("fee_rate")
                    .oldFeeRate(config.getFeeRate())
                    .newFeeRate(req.getFeeRate())
                    .reason(req.getReason())
                    .changedBy(username)
                    .build());

            config.setFeeRate(req.getFeeRate());
        }

        if (req.getStatus() != null && req.getStatus() != config.getStatus()) {
            recordHistory(FeeConfigHistory.builder()
                    .feeConfigurationId(id)
                    .merchantId(findMerchantIdByEntityId(config.getEntityId()))
                    .action("UPDATE")
                    .fieldName("status")
                    .oldStatus(config.getStatus().name())
                    .newStatus(req.getStatus().name())
                    .reason(req.getReason())
                    .changedBy(username)
                    .build());

            config.setStatus(req.getStatus());
        }

        if (req.getFeeType() != null) {
            config.setFeeType(req.getFeeType());
        }
        if (req.getFixedFee() != null) {
            config.setFixedFee(req.getFixedFee());
        }
        if (req.getMinFee() != null) {
            config.setMinFee(req.getMinFee());
        }
        if (req.getMaxFee() != null) {
            config.setMaxFee(req.getMaxFee());
        }
        if (req.getPriority() != null) {
            config.setPriority(req.getPriority());
        }
        if (req.getPaymentMethodId() != null) {
            config.setPaymentMethodId(req.getPaymentMethodId());
        }
        if (req.getValidFrom() != null) {
            config.setValidFrom(req.getValidFrom());
        }
        if (req.getValidUntil() != null) {
            config.setValidUntil(req.getValidUntil());
        }

        return feeConfigurationRepository.save(config);
    }

    @Transactional
    public void deactivate(UUID id, String reason) {
        FeeConfiguration config = getById(id);

        recordHistory(FeeConfigHistory.builder()
                .feeConfigurationId(id)
                .merchantId(findMerchantIdByEntityId(config.getEntityId()))
                .action("DEACTIVATE")
                .oldStatus(config.getStatus().name())
                .newStatus(FeeConfigStatus.INACTIVE.name())
                .reason(reason)
                .changedBy(getCurrentUsername())
                .build());

        config.setStatus(FeeConfigStatus.INACTIVE);
        feeConfigurationRepository.save(config);
    }

    @Transactional
    public void activate(UUID id, String reason) {
        FeeConfiguration config = getById(id);

        recordHistory(FeeConfigHistory.builder()
                .feeConfigurationId(id)
                .merchantId(findMerchantIdByEntityId(config.getEntityId()))
                .action("ACTIVATE")
                .oldStatus(config.getStatus().name())
                .newStatus(FeeConfigStatus.ACTIVE.name())
                .reason(reason)
                .changedBy(getCurrentUsername())
                .build());

        config.setStatus(FeeConfigStatus.ACTIVE);
        feeConfigurationRepository.save(config);
    }

    public List<FeeConfigHistory> getHistory(UUID feeConfigId) {
        return feeConfigHistoryRepository.findByFeeConfigurationIdOrderByChangedAtDesc(feeConfigId);
    }

    public List<FeeConfigHistory> getMerchantHistory(UUID merchantId) {
        return feeConfigHistoryRepository.findByMerchantIdOrderByChangedAtDesc(merchantId);
    }

    public String resolvePaymentMethodName(UUID paymentMethodId) {
        if (paymentMethodId == null) return null;
        return paymentMethodRepository.findById(paymentMethodId)
                .map(PaymentMethod::getName)
                .orElse(null);
    }

    private Merchant findMerchant(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + merchantId));
    }

    private UUID findMerchantIdByEntityId(UUID entityId) {
        List<Merchant> merchants = merchantRepository.findByOrganizationId(entityId);
        return merchants.isEmpty() ? null : merchants.getFirst().getId();
    }

    private void recordHistory(FeeConfigHistory history) {
        feeConfigHistoryRepository.save(history);
    }

    private String getCurrentUsername() {
        try {
            return userContextHolder.getCurrentUser().getUsername();
        } catch (Exception e) {
            return "system";
        }
    }

    private void validateFeeRate(FeeType feeType, BigDecimal feeRate) {
        if (feeType == FeeType.PERCENTAGE && feeRate != null) {
            if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("Fee rate must be between 0 and 1 for PERCENTAGE type");
            }
        }
    }
}
