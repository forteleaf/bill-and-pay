package com.korpay.billpay.exception.webhook;

/**
 * Base exception for webhook processing errors
 */
public class WebhookException extends RuntimeException {

    public WebhookException(String message) {
        super(message);
    }

    public WebhookException(String message, Throwable cause) {
        super(message, cause);
    }
}
