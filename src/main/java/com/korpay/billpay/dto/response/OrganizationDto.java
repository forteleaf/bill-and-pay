package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.enums.OrganizationStatus;
import com.korpay.billpay.domain.enums.OrganizationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {
    
    private UUID id;
    private String orgCode;
    private String name;
    private OrganizationType orgType;
    private String path;
    private Integer level;
    private UUID parentId;
    private String businessNumber;
    private String businessName;
    private String representativeName;
    private String email;
    private String phone;
    private String address;
    private OrganizationStatus status;
    private Map<String, Object> config;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public static OrganizationDto from(Organization organization) {
        return OrganizationDto.builder()
                .id(organization.getId())
                .orgCode(organization.getOrgCode())
                .name(organization.getName())
                .orgType(organization.getOrgType())
                .path(organization.getPath())
                .level(organization.getLevel())
                .parentId(organization.getParent() != null ? organization.getParent().getId() : null)
                .businessNumber(organization.getBusinessNumber())
                .businessName(organization.getBusinessName())
                .representativeName(organization.getRepresentativeName())
                .email(organization.getEmail())
                .phone(organization.getPhone())
                .address(organization.getAddress())
                .status(organization.getStatus())
                .config(organization.getConfig())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }
}
