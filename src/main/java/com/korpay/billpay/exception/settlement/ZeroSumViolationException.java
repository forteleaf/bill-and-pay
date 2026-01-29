package com.korpay.billpay.exception.settlement;

import java.util.UUID;

/**
 * Exception thrown when Zero-Sum validation fails.
 * <p>
 * Zero-Sum Principle: |event amount| = SUM(settlement amounts)
 * This is a critical invariant in double-entry bookkeeping.
 */
public class ZeroSumViolationException extends SettlementCalculationException {

    private final UUID transactionEventId;
    private final long eventAmount;
    private final long settlementTotal;
    private final long difference;

    public ZeroSumViolationException(UUID transactionEventId, long eventAmount, long settlementTotal) {
        super(String.format(
                "Zero-Sum validation failed for event %s: event amount=%d, settlement total=%d, difference=%d",
                transactionEventId, eventAmount, settlementTotal, Math.abs(eventAmount) - settlementTotal
        ));
        this.transactionEventId = transactionEventId;
        this.eventAmount = eventAmount;
        this.settlementTotal = settlementTotal;
        this.difference = Math.abs(eventAmount) - settlementTotal;
    }

    public UUID getTransactionEventId() {
        return transactionEventId;
    }

    public long getEventAmount() {
        return eventAmount;
    }

    public long getSettlementTotal() {
        return settlementTotal;
    }

    public long getDifference() {
        return difference;
    }
}
