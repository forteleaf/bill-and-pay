package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.PgConnectionStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "pg_connections", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PgConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pg_code", nullable = false, unique = true, length = 20)
    private String pgCode;

    @Column(name = "pg_name", nullable = false, length = 100)
    private String pgName;

    @Column(name = "api_base_url", nullable = false, length = 500)
    private String apiBaseUrl;

    @Column(name = "webhook_base_url", length = 500)
    private String webhookBaseUrl;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /** JSONB: {api_key, secret_key, merchant_id} */
    @Type(JsonBinaryType.class)
    @Column(name = "credentials", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> credentials;

    /** JSONB: {timeout_ms, retry_count, webhook_secret} */
    @Type(JsonBinaryType.class)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PgConnectionStatus status = PgConnectionStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public String getApiKey() {
        return credentials != null ? (String) credentials.get("api_key") : null;
    }

    public String getSecretKey() {
        return credentials != null ? (String) credentials.get("secret_key") : null;
    }

    public String getMerchantId() {
        return credentials != null ? (String) credentials.get("merchant_id") : null;
    }

    public String getWebhookSecret() {
        return config != null ? (String) config.get("webhook_secret") : null;
    }

    public Integer getTimeoutMs() {
        if (config == null || !config.containsKey("timeout_ms")) return null;
        Object val = config.get("timeout_ms");
        return val instanceof Number ? ((Number) val).intValue() : null;
    }

    public Integer getRetryCount() {
        if (config == null || !config.containsKey("retry_count")) return null;
        Object val = config.get("retry_count");
        return val instanceof Number ? ((Number) val).intValue() : null;
    }
}
