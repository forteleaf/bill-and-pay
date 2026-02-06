package com.korpay.billpay.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SettlementCycle {
    D_PLUS_1(1),
    D_PLUS_3(3),
    REALTIME(0);

    private final int businessDays;
}
