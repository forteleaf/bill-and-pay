package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.AnnouncementSeverity;
import com.korpay.billpay.domain.enums.AnnouncementStatus;
import com.korpay.billpay.domain.enums.AnnouncementType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "platform_announcements", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAnnouncement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "announcement_type", nullable = false, length = 30)
    private AnnouncementType announcementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private AnnouncementSeverity severity = AnnouncementSeverity.INFO;

    @Column(name = "target_type", nullable = false, length = 20)
    @Builder.Default
    private String targetType = "ALL";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_tenant_ids", columnDefinition = "jsonb")
    private List<String> targetTenantIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    @Column(name = "publish_at")
    private OffsetDateTime publishAt;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
