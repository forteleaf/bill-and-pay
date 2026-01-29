package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.UserStatus;
import com.korpay.billpay.domain.type.LtreeType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Type(LtreeType.class)
    @Column(name = "org_path", nullable = false, columnDefinition = "ltree")
    private String orgPath;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Type(JsonBinaryType.class)
    @Column(name = "permissions", columnDefinition = "jsonb")
    private List<String> permissions;

    @Column(name = "two_factor_enabled", nullable = false)
    private Boolean twoFactorEnabled;

    @Column(name = "two_factor_secret", length = 255)
    private String twoFactorSecret;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private OffsetDateTime passwordChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
