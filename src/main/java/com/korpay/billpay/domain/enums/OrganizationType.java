package com.korpay.billpay.domain.enums;

/**
 * Organization hierarchy type (5 levels)
 * Level 1: DISTRIBUTOR (총판)
 * Level 2: AGENCY (대리점)
 * Level 3: DEALER (딜러)
 * Level 4: SELLER (셀러)
 * Level 5: VENDOR (벤더)
 */
public enum OrganizationType {
    DISTRIBUTOR,
    AGENCY,
    DEALER,
    SELLER,
    VENDOR
}
