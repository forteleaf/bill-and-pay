package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.OrganizationStatus;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.type.LtreeType;
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
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_code", nullable = false, unique = true, length = 50)
    private String orgCode;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false, length = 20)
    private OrganizationType orgType;

    @Type(LtreeType.class)
    @Column(name = "path", nullable = false, unique = true, columnDefinition = "ltree")
    private String path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Organization parent;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "representative_name", length = 100)
    private String representativeName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationStatus status;

    @Type(JsonBinaryType.class)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
