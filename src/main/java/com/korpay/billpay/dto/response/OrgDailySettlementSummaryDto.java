package com.korpay.billpay.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record OrgDailySettlementSummaryDto(
    LocalDate settlementDate,
    OffsetDateTime periodStart,
    OffsetDateTime periodEnd,
    long orgCount,
    long transactionCount,
    long approvalCount,
    long approvalAmount,
    long cancelCount,
    long cancelAmount,
    long feeAmount,
    long netAmount,
    String status
) {}
