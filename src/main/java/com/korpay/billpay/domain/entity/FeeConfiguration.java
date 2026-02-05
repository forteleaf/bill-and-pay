package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.FeeConfigStatus;
import com.korpay.billpay.domain.enums.FeeType;
import com.korpay.billpay.domain.enums.OrganizationType;
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
@Table(name = "fee_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FeeConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private OrganizationType entityType;

    @Type(LtreeType.class)
    @Column(name = "entity_path", nullable = false, columnDefinition = "ltree")
    private String entityPath;

    @Column(name = "payment_method_id")
    private UUID paymentMethodId;

    @Column(name = "card_company_id")
    private UUID cardCompanyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType;

    @Column(name = "fee_rate", precision = 10, scale = 6)
    private BigDecimal feeRate;

    @Column(name = "fixed_fee")
    private Long fixedFee;

    @Type(JsonBinaryType.class)
    @Column(name = "tier_config", columnDefinition = "jsonb")
    private Map<String, Object> tierConfig;

    @Column(name = "min_fee")
    private Long minFee;

    @Column(name = "max_fee")
    private Long maxFee;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_until")
    private OffsetDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeConfigStatus status;

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
