package com.korpay.billpay.service.webhook.adapter;

import com.korpay.billpay.dto.webhook.TransactionDto;

import java.util.Map;

public interface PgWebhookAdapter {

    TransactionDto parse(String rawBody, Map<String, String> headers);

    boolean verifySignature(String rawBody, Map<String, String> headers, String secret);

    String getPgCode();
}
