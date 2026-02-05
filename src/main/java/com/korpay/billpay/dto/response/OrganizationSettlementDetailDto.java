package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.enums.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettlementDetailDto {
    
    private UUID organizationId;
    private String orgCode;
    private String orgName;
    private OrganizationType orgType;
    private String orgPath;
    
    private OrganizationSettlementSummaryDto summary;
    
    private List<MerchantSettlementDto> merchantSettlements;
    
    private List<HierarchyFeeDto> hierarchyFees;
    
    private SettlementCalculationDto calculation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerchantSettlementDto {
        private UUID merchantId;
        private String merchantCode;
        private String merchantName;
        private LocalDate transactionDate;
        private String branchName;
        private Long approvalAmount;
        private Long approvalCount;
        private Long cancelAmount;
        private Long cancelCount;
        private Long netPaymentAmount;
        private Long feeAmount;
        private BigDecimal feeRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HierarchyFeeDto {
        private OrganizationType entityType;
        private String entityName;
        private String entityCode;
        private BigDecimal feeRate;
        private Long feeAmount;
        private Long netAmount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementCalculationDto {
        private BigDecimal settlementFeeRate;
        private Long grossAmount;
        private Long supplyAmount;
        private Long vatAmount;
        private Long finalAmount;
        private Long childOrgFeeAmount;
        private Long merchantPayoutAmount;
    }
}
