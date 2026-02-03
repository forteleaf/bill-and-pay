package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.TerminalStatus;
import com.korpay.billpay.domain.enums.TerminalType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "terminals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Terminal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tid", nullable = false, unique = true, length = 50)
    private String tid;

    @Column(name = "cat_id", length = 50)
    private String catId;

    @Enumerated(EnumType.STRING)
    @Column(name = "terminal_type", nullable = false, length = 20)
    private TerminalType terminalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "model", length = 100)
    private String model;

    @Column(name = "manufacturer", length = 100)
    private String manufacturer;

    @Column(name = "install_address", columnDefinition = "TEXT")
    private String installAddress;

    @Column(name = "install_date")
    private LocalDate installDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TerminalStatus status = TerminalStatus.ACTIVE;

    @Type(JsonBinaryType.class)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "last_transaction_at")
    private OffsetDateTime lastTransactionAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
