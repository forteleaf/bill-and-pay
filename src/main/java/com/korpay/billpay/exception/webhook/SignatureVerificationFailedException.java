package com.korpay.billpay.exception.webhook;

/**
 * Exception thrown when webhook signature verification fails
 * This indicates a potential security issue or misconfiguration
 */
public class SignatureVerificationFailedException extends WebhookException {

    public SignatureVerificationFailedException(String message) {
        super(message);
    }

    public SignatureVerificationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
