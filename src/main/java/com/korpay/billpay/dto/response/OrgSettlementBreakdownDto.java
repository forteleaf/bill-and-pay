package com.korpay.billpay.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrgSettlementBreakdownDto(
    UUID orgId,
    String orgName,
    String orgType,
    String orgCode,
    long transactionCount,
    long approvalCount,
    long approvalAmount,
    long cancelCount,
    long cancelAmount,
    BigDecimal feeRate,
    long feeAmount,
    long netAmount
) {}
