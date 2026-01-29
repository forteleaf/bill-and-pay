package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.enums.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementSummaryDto {
    
    private UUID entityId;
    private OrganizationType entityType;
    private String entityPath;
    private Long totalAmount;
    private Long totalFeeAmount;
    private Long totalNetAmount;
    private Long creditAmount;
    private Long debitAmount;
    private Long transactionCount;
    private String currency;
    private Map<String, Object> breakdown;
}
