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
import java.util.UUID;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pg_code", nullable = false, unique = true, length = 20)
    private String pgCode;

    @Column(name = "pg_name", nullable = false, length = 50)
    private String pgName;

    @Column(name = "pg_api_version", length = 20)
    private String pgApiVersion;

    @Column(name = "merchant_id", nullable = false, length = 100)
    private String merchantId;

    @Column(name = "api_key_enc", nullable = false)
    private byte[] apiKeyEnc;

    @Column(name = "api_secret_enc", nullable = false)
    private byte[] apiSecretEnc;

    @Column(name = "webhook_path", nullable = false, unique = true, length = 100)
    private String webhookPath;

    @Column(name = "webhook_secret", length = 100)
    private String webhookSecret;

    @Column(name = "api_base_url", length = 200)
    private String apiBaseUrl;

    @Type(JsonBinaryType.class)
    @Column(name = "api_endpoints", columnDefinition = "jsonb")
    private Map<String, String> apiEndpoints;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PgConnectionStatus status = PgConnectionStatus.ACTIVE;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
