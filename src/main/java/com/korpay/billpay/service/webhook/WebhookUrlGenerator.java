package com.korpay.billpay.service.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class WebhookUrlGenerator {

    private final String webhookBaseUrl;

    public WebhookUrlGenerator(@Value("${app.webhook.base-url:http://localhost:8080/api}") String webhookBaseUrl) {
        this.webhookBaseUrl = webhookBaseUrl.endsWith("/") 
            ? webhookBaseUrl.substring(0, webhookBaseUrl.length() - 1) 
            : webhookBaseUrl;
    }

    public String generateUrl(String tenantId, String pgCode, Long pgConnectionId, String webhookSecret) {
        StringBuilder url = new StringBuilder(webhookBaseUrl)
            .append("/webhook/")
            .append(tenantId)
            .append("/")
            .append(pgCode)
            .append("?pgConnectionId=")
            .append(pgConnectionId);
        
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            url.append("&webhookSecret=").append(webhookSecret);
        }
        
        return url.toString();
    }

    public String getBaseUrl() {
        return webhookBaseUrl;
    }
}
