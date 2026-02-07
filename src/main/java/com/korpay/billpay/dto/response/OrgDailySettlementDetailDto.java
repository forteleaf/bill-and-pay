package com.korpay.billpay.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record OrgDailySettlementDetailDto(
    LocalDate settlementDate,
    String batchNumber,
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd,
    SummaryDto summary,
    List<OrgSettlementBreakdownDto> orgBreakdown,
    List<SettlementDto> settlements
) {
    public record SummaryDto(
        long transactionCount,
        long approvalAmount,
        long cancelAmount,
        long feeAmount,
        long netAmount
    ) {}
}
