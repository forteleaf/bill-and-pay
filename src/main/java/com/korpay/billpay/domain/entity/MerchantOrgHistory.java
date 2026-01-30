package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.type.LtreeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchant_org_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MerchantOrgHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "from_org_id", nullable = false)
    private UUID fromOrgId;

    @Type(LtreeType.class)
    @Column(name = "from_org_path", nullable = false, columnDefinition = "ltree")
    private String fromOrgPath;

    @Column(name = "to_org_id", nullable = false)
    private UUID toOrgId;

    @Type(LtreeType.class)
    @Column(name = "to_org_path", nullable = false, columnDefinition = "ltree")
    private String toOrgPath;

    @Column(name = "moved_at", nullable = false)
    private Instant movedAt;

    @Column(name = "moved_by", length = 100)
    private String movedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.movedAt == null) {
            this.movedAt = Instant.now();
        }
    }

    @Builder
    public MerchantOrgHistory(UUID merchantId, UUID fromOrgId, String fromOrgPath,
                              UUID toOrgId, String toOrgPath, String movedBy, String reason) {
        this.merchantId = merchantId;
        this.fromOrgId = fromOrgId;
        this.fromOrgPath = fromOrgPath;
        this.toOrgId = toOrgId;
        this.toOrgPath = toOrgPath;
        this.movedBy = movedBy;
        this.reason = reason;
    }
}
