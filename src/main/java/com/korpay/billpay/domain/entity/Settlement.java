package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.domain.type.LtreeType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_batch_id")
    private SettlementBatch settlementBatch;

    @Column(name = "transaction_event_id", nullable = false)
    private UUID transactionEventId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Type(LtreeType.class)
    @Column(name = "org_path", nullable = false, columnDefinition = "ltree")
    private String orgPath;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private OrganizationType entityType;

    @Type(LtreeType.class)
    @Column(name = "entity_path", nullable = false, columnDefinition = "ltree")
    private String entityPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "fee_amount", nullable = false)
    private Long feeAmount;

    @Column(name = "net_amount", nullable = false)
    private Long netAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "fee_rate", precision = 10, scale = 6)
    private BigDecimal feeRate;

    @Type(JsonBinaryType.class)
    @Column(name = "fee_config", columnDefinition = "jsonb")
    private Map<String, Object> feeConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
