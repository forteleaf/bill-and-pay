package com.korpay.billpay.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {

    private boolean success;
    private String message;
    private String transactionId;

    public static WebhookResponse success(String transactionId) {
        return WebhookResponse.builder()
                .success(true)
                .message("Webhook processed successfully")
                .transactionId(transactionId)
                .build();
    }

    public static WebhookResponse error(String message) {
        return WebhookResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    public static WebhookResponse duplicate(String transactionId) {
        return WebhookResponse.builder()
                .success(true)
                .message("Transaction already processed")
                .transactionId(transactionId)
                .build();
    }
}
