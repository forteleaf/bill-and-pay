package com.korpay.billpay.exception.settlement;

/**
 * Base exception for settlement calculation errors.
 * <p>
 * Settlement calculations involve complex fee distribution logic
 * and must maintain strict financial invariants.
 */
public class SettlementCalculationException extends RuntimeException {

    public SettlementCalculationException(String message) {
        super(message);
    }

    public SettlementCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
