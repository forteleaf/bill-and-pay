package com.korpay.billpay.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fee_configuration_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "fee_configuration_id", nullable = false)
    private UUID feeConfigurationId;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @Column(name = "field_name", length = 50)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "old_fee_rate", precision = 10, scale = 6)
    private BigDecimal oldFeeRate;

    @Column(name = "new_fee_rate", precision = 10, scale = 6)
    private BigDecimal newFeeRate;

    @Column(name = "old_status", length = 20)
    private String oldStatus;

    @Column(name = "new_status", length = 20)
    private String newStatus;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        if (this.changedAt == null) {
            this.changedAt = OffsetDateTime.now();
        }
    }

    @Builder
    public FeeConfigHistory(UUID feeConfigurationId, UUID merchantId, String action,
                            String fieldName, String oldValue, String newValue,
                            BigDecimal oldFeeRate, BigDecimal newFeeRate,
                            String oldStatus, String newStatus,
                            String reason, String changedBy) {
        this.feeConfigurationId = feeConfigurationId;
        this.merchantId = merchantId;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.oldFeeRate = oldFeeRate;
        this.newFeeRate = newFeeRate;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
    }
}
