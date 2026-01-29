package com.korpay.billpay.dto.settlement;

import com.korpay.billpay.domain.enums.OrganizationType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class FeeBreakdown {
    private UUID entityId;
    private OrganizationType entityType;
    private String entityPath;
    private BigDecimal feeRate;
    private BigDecimal marginRate;
    private Long marginAmount;
    private Long settlementAmount;
    private String description;
}
