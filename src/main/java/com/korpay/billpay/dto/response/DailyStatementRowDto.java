package com.korpay.billpay.dto.response;

import java.time.LocalDate;

public record DailyStatementRowDto(
    LocalDate settlementDate,
    LocalDate transactionDate,
    long transactionCount,
    long approvalCount,
    long approvalAmount,
    long cancelCount,
    long cancelAmount,
    long feeAmount,
    long netAmount
) {}
