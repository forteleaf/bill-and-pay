package com.korpay.billpay.exception.webhook;

/**
 * Generic exception for webhook processing errors
 * Should result in 500 response to trigger PG retry
 */
public class WebhookProcessingException extends WebhookException {

    public WebhookProcessingException(String message) {
        super(message);
    }

    public WebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
