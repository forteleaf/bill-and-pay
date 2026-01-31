package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.BusinessEntity;
import com.korpay.billpay.domain.enums.BusinessType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessEntityResponse {

    private UUID id;
    private BusinessType businessType;
    private String businessNumber;
    private String corporateNumber;
    private String businessName;
    private String representativeName;
    private LocalDate openDate;
    private String businessAddress;
    private String actualAddress;
    private String businessCategory;
    private String businessSubCategory;
    private String mainPhone;
    private String managerName;
    private String managerPhone;
    private String email;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static BusinessEntityResponse from(BusinessEntity entity) {
        return BusinessEntityResponse.builder()
                .id(entity.getId())
                .businessType(entity.getBusinessType())
                .businessNumber(entity.getBusinessNumber())
                .corporateNumber(entity.getCorporateNumber())
                .businessName(entity.getBusinessName())
                .representativeName(entity.getRepresentativeName())
                .openDate(entity.getOpenDate())
                .businessAddress(entity.getBusinessAddress())
                .actualAddress(entity.getActualAddress())
                .businessCategory(entity.getBusinessCategory())
                .businessSubCategory(entity.getBusinessSubCategory())
                .mainPhone(entity.getMainPhone())
                .managerName(entity.getManagerName())
                .managerPhone(entity.getManagerPhone())
                .email(entity.getEmail())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
