package com.korpay.billpay.domain.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "unmapped_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnmappedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pg_connection_id", nullable = false)
    private Long pgConnectionId;

    @Column(name = "pg_tid", nullable = false, length = 200)
    private String pgTid;

    @Column(name = "pg_merchant_no", nullable = false, length = 50)
    private String pgMerchantNo;

    @Type(JsonBinaryType.class)
    @Column(name = "raw_data", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "transacted_at", nullable = false)
    private OffsetDateTime transactedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "mapped_merchant_id")
    private UUID mappedMerchantId;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "process_note")
    private String processNote;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @PrePersist
    public void prePersist() {
        if (this.receivedAt == null) {
            this.receivedAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
