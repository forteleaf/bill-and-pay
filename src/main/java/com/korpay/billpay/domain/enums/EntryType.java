package com.korpay.billpay.domain.enums;

/**
 * Double-entry bookkeeping entry type
 * CREDIT: money in (approval)
 * DEBIT: money out (cancel/refund)
 */
public enum EntryType {
    CREDIT,
    DEBIT
}
