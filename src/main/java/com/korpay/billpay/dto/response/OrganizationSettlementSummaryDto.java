package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettlementSummaryDto {
    
    private UUID organizationId;
    private String orgCode;
    private String orgName;
    private OrganizationType orgType;
    private String orgPath;
    private Integer level;
    
    private UUID businessEntityId;
    private String representativeName;
    private String mainPhone;
    
    private Long merchantCount;
    
    private Long approvalAmount;
    private Long approvalCount;
    private Long cancelAmount;
    private Long cancelCount;
    private Long netPaymentAmount;
    private Long totalTransactionCount;
    
    private Long merchantFeeAmount;
    private Long orgFeeAmount;
    private BigDecimal avgFeeRate;
    
    private SettlementStatus primaryStatus;
    private Long completedCount;
    private Long pendingCount;
    private Long failedCount;
    
    private String currency;
}
