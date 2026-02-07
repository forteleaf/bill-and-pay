package com.korpay.billpay.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Entity
@Table(name = "webhook_idempotency_keys")
@IdClass(WebhookIdempotencyKey.WebhookIdempotencyKeyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookIdempotencyKey {

    @Id
    @Column(name = "pg_connection_id", nullable = false)
    private Long pgConnectionId;

    @Id
    @Column(name = "pg_tid", nullable = false, length = 200)
    private String pgTid;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "PROCESSING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookIdempotencyKeyId implements Serializable {
        private Long pgConnectionId;
        private String pgTid;
    }
}
