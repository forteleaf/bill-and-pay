package com.korpay.billpay.exception.settlement;

import java.util.UUID;

/**
 * Exception thrown when fee configuration is missing or invalid.
 */
public class FeeConfigNotFoundException extends SettlementCalculationException {

    private final UUID entityId;
    private final String entityType;
    private final String paymentMethod;

    public FeeConfigNotFoundException(UUID entityId, String entityType, String paymentMethod) {
        super(String.format(
                "Fee configuration not found for entity %s (type=%s) and payment method=%s",
                entityId, entityType, paymentMethod
        ));
        this.entityId = entityId;
        this.entityType = entityType;
        this.paymentMethod = paymentMethod;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}
