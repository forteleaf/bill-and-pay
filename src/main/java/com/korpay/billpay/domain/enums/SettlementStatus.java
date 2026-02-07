package com.korpay.billpay.domain.enums;

public enum SettlementStatus {
    PENDING,
    PENDING_REVIEW,  // Zero-Sum 검증 실패 시 수동 검토 대기
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
