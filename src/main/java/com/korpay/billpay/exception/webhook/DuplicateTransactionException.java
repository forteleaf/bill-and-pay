package com.korpay.billpay.exception.webhook;

/**
 * Exception thrown when attempting to process a duplicate transaction
 * This is used for idempotency checking - should return 200 OK to PG
 */
public class DuplicateTransactionException extends WebhookException {

    private final String pgTid;

    public DuplicateTransactionException(String pgTid) {
        super(String.format("Transaction already processed: %s", pgTid));
        this.pgTid = pgTid;
    }

    public String getPgTid() {
        return pgTid;
    }
}
