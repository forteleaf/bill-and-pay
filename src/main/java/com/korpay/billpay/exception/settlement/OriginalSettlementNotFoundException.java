package com.korpay.billpay.exception.settlement;

import java.util.UUID;

/**
 * Exception thrown when original approval settlement is not found for partial cancel calculation.
 */
public class OriginalSettlementNotFoundException extends SettlementCalculationException {

    private final UUID transactionId;
    private final UUID cancelEventId;

    public OriginalSettlementNotFoundException(UUID transactionId, UUID cancelEventId) {
        super(String.format(
                "Original approval settlement not found for transaction %s when processing cancel event %s",
                transactionId, cancelEventId
        ));
        this.transactionId = transactionId;
        this.cancelEventId = cancelEventId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getCancelEventId() {
        return cancelEventId;
    }
}
