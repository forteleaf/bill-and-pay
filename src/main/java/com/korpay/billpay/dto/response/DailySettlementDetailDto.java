package com.korpay.billpay.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DailySettlementDetailDto(
    LocalDate settlementDate,
    String batchNumber,
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd,
    SummaryDto summary,
    List<MerchantSettlementBreakdownDto> merchantBreakdown,
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
