package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.EventType;
import com.korpay.billpay.domain.type.LtreeType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "transaction_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TransactionEvent.TransactionEventId.class)
public class TransactionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Id
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "event_sequence", nullable = false)
    private Integer eventSequence;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "merchant_pg_mapping_id", nullable = false)
    private UUID merchantPgMappingId;

    @Column(name = "pg_connection_id", nullable = false)
    private Long pgConnectionId;

    @Type(LtreeType.class)
    @Column(name = "merchant_path", nullable = false, columnDefinition = "ltree")
    private String merchantPath;

    @Type(LtreeType.class)
    @Column(name = "org_path", nullable = false, columnDefinition = "ltree")
    private String orgPath;

    @Column(name = "payment_method_id", nullable = false)
    private UUID paymentMethodId;

    @Column(name = "card_company_id")
    private UUID cardCompanyId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "pg_transaction_id", length = 200)
    private String pgTransactionId;

    @Column(name = "approval_number", length = 50)
    private String approvalNumber;

    @Column(name = "cat_id", length = 50)
    private String catId;

    @Column(name = "tid", length = 100)
    private String tid;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionEventId implements Serializable {
        private UUID id;
        private OffsetDateTime createdAt;
    }
}
