package com.korpay.billpay.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantSettlementBreakdownDto(
    UUID merchantId,
    String merchantName,
    String merchantCode,
    long transactionCount,
    long approvalCount,
    long approvalAmount,
    long cancelCount,
    long cancelAmount,
    BigDecimal feeRate,
    long feeAmount,
    long netAmount,
    String orgCode,
    String settlementCycle,
    String paymentType,
    String bankName,
    String accountNumber,
    String accountHolder,
    String status
) {}
