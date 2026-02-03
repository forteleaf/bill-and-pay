package com.korpay.billpay.service.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates webhook URLs for PG provider configuration.
 * 
 * <p>Supports two URL formats:
 * <ul>
 *   <li><b>New format</b>: {@code /webhook/{tenantId}/{pgCode}?pgConnectionId=...&webhookSecret=...}</li>
 *   <li><b>Legacy format</b>: {@code /webhook/{pgCode}?pgConnectionId=...&webhookSecret=...}</li>
 * </ul>
 * 
 * <p>The new format is recommended for all new PG integrations as it enables
 * proper multi-tenant isolation at the URL level.
 */
@Service
public class WebhookUrlGenerator {

    private final String webhookBaseUrl;

    public WebhookUrlGenerator(@Value("${app.webhook.base-url:http://localhost:8080/api}") String webhookBaseUrl) {
        this.webhookBaseUrl = webhookBaseUrl.endsWith("/") 
            ? webhookBaseUrl.substring(0, webhookBaseUrl.length() - 1) 
            : webhookBaseUrl;
    }

    /**
     * Generates a tenant-aware webhook URL (recommended for new integrations).
     * 
     * <p>Format: {@code {baseUrl}/webhook/{tenantId}/{pgCode}?pgConnectionId={id}&webhookSecret={secret}}
     *
     * @param tenantId tenant identifier
     * @param pgCode PG provider code (e.g., "KORPAY")
     * @param pgConnectionId PG connection ID
     * @param webhookSecret webhook secret for signature verification
     * @return fully qualified webhook URL
     */
    public String generateNewUrl(String tenantId, String pgCode, Long pgConnectionId, String webhookSecret) {
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

    /**
     * Generates a legacy webhook URL (deprecated, for backward compatibility).
     * 
     * <p>Format: {@code {baseUrl}/webhook/{pgCode}?pgConnectionId={id}&webhookSecret={secret}}
     * 
     * <p><b>Note:</b> This format is deprecated. The tenant will be derived from
     * the pg_connections.tenant_id column. Use {@link #generateNewUrl} for new integrations.
     *
     * @param pgCode PG provider code (e.g., "KORPAY")
     * @param pgConnectionId PG connection ID
     * @param webhookSecret webhook secret for signature verification
     * @return fully qualified webhook URL (legacy format)
     */
    public String generateLegacyUrl(String pgCode, Long pgConnectionId, String webhookSecret) {
        StringBuilder url = new StringBuilder(webhookBaseUrl)
            .append("/webhook/")
            .append(pgCode)
            .append("?pgConnectionId=")
            .append(pgConnectionId);
        
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            url.append("&webhookSecret=").append(webhookSecret);
        }
        
        return url.toString();
    }

    /**
     * Returns the configured webhook base URL.
     * 
     * @return base URL for webhook endpoints
     */
    public String getBaseUrl() {
        return webhookBaseUrl;
    }
}
