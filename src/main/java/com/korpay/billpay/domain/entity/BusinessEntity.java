package com.korpay.billpay.domain.entity;

import com.korpay.billpay.domain.enums.BusinessType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "business_entities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BusinessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", nullable = false, length = 20)
    private BusinessType businessType;

    @Column(name = "business_number", length = 12)
    private String businessNumber;

    @Column(name = "corporate_number", length = 14)
    private String corporateNumber;

    @Column(name = "business_name", nullable = false, length = 200)
    private String businessName;

    @Column(name = "representative_name", nullable = false, length = 100)
    private String representativeName;

    @Column(name = "open_date")
    private LocalDate openDate;

    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;

    @Column(name = "actual_address", columnDefinition = "TEXT")
    private String actualAddress;

    @Column(name = "business_category", length = 100)
    private String businessCategory;

    @Column(name = "business_sub_category", length = 100)
    private String businessSubCategory;

    @Column(name = "main_phone", length = 20)
    private String mainPhone;

    @Column(name = "manager_name", length = 100)
    private String managerName;

    @Column(name = "manager_phone", length = 20)
    private String managerPhone;

    @Column(name = "email", length = 255)
    private String email;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
