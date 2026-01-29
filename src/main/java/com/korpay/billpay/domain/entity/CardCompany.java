package com.korpay.billpay.domain.entity;

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
@Table(name = "card_companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class CardCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_code", nullable = false, unique = true, length = 10)
    private String companyCode;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Type(JsonBinaryType.class)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
