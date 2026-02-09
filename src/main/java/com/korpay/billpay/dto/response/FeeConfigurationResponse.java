package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.FeeConfiguration;
import com.korpay.billpay.domain.enums.FeeConfigStatus;
import com.korpay.billpay.domain.enums.FeeType;
import com.korpay.billpay.domain.enums.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeConfigurationResponse {

    private UUID id;
    private UUID entityId;
    private OrganizationType entityType;
    private String entityPath;
    private UUID paymentMethodId;
    private String paymentMethodName;
    private UUID cardCompanyId;
    private FeeType feeType;
    private BigDecimal feeRate;
    private Long fixedFee;
    private Map<String, Object> tierConfig;
    private Long minFee;
    private Long maxFee;
    private Integer priority;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private FeeConfigStatus status;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static FeeConfigurationResponse from(FeeConfiguration fc, String paymentMethodName) {
        return FeeConfigurationResponse.builder()
                .id(fc.getId())
                .entityId(fc.getEntityId())
                .entityType(fc.getEntityType())
                .entityPath(fc.getEntityPath())
                .paymentMethodId(fc.getPaymentMethodId())
                .paymentMethodName(paymentMethodName)
                .cardCompanyId(fc.getCardCompanyId())
                .feeType(fc.getFeeType())
                .feeRate(fc.getFeeRate())
                .fixedFee(fc.getFixedFee())
                .tierConfig(fc.getTierConfig())
                .minFee(fc.getMinFee())
                .maxFee(fc.getMaxFee())
                .priority(fc.getPriority())
                .validFrom(fc.getValidFrom())
                .validUntil(fc.getValidUntil())
                .status(fc.getStatus())
                .metadata(fc.getMetadata())
                .createdAt(fc.getCreatedAt())
                .updatedAt(fc.getUpdatedAt())
                .build();
    }
}
