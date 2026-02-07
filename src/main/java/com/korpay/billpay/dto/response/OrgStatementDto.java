package com.korpay.billpay.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrgStatementDto(
    UUID orgId,
    String orgName,
    String orgCode,
    String orgType,
    String businessNumber,
    String representativeName,
    String settlementCycle,
    String bankName,
    String accountNumber,
    String accountHolder,
    String periodStart,
    String periodEnd,
    SummaryDto summary,
    List<DailyOrgStatementRowDto> dailyDetails
) {
    public record SummaryDto(
        long totalApprovalAmount,
        long totalApprovalCount,
        long totalCancelAmount,
        long totalCancelCount,
        long grossAmount,
        BigDecimal feeRate,
        long feeAmount,
        long netAmount,
        long transactionCount
    ) {}
}
