package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.WebhookLogStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "webhook_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pg_connection_id", nullable = false)
    private Long pgConnectionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Type(JsonBinaryType.class)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Type(JsonBinaryType.class)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Map<String, String> headers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WebhookLogStatus status = WebhookLogStatus.RECEIVED;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "transaction_event_id")
    private UUID transactionEventId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "signature", length = 500)
    private String signature;

    @Column(name = "signature_verified")
    private Boolean signatureVerified;

    @Column(name = "received_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime receivedAt = OffsetDateTime.now();

    public void markProcessing() {
        this.status = WebhookLogStatus.PROCESSING;
    }

    public void markProcessed(UUID transactionId, UUID transactionEventId) {
        this.status = WebhookLogStatus.PROCESSED;
        this.processedAt = OffsetDateTime.now();
        this.transactionId = transactionId;
        this.transactionEventId = transactionEventId;
    }

    public void markFailed(String errorMessage) {
        this.status = WebhookLogStatus.FAILED;
        this.processedAt = OffsetDateTime.now();
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    public void markIgnored(String reason) {
        this.status = WebhookLogStatus.IGNORED;
        this.processedAt = OffsetDateTime.now();
        this.errorMessage = reason;
    }

    public boolean canRetry(int maxRetries) {
        return this.status == WebhookLogStatus.FAILED && this.retryCount < maxRetries;
    }
}
