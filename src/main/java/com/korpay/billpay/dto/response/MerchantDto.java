package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.enums.MerchantStatus;
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
public class MerchantDto {
    
    private UUID id;
    private String merchantCode;
    private String name;
    private UUID organizationId;
    private String orgPath;
    private String businessNumber;
    private String businessType;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private MerchantStatus status;
    private Map<String, Object> config;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public static MerchantDto from(Merchant merchant) {
        return MerchantDto.builder()
                .id(merchant.getId())
                .merchantCode(merchant.getMerchantCode())
                .name(merchant.getName())
                .organizationId(merchant.getOrganization().getId())
                .orgPath(merchant.getOrgPath())
                .businessNumber(merchant.getBusinessNumber())
                .businessType(merchant.getBusinessType())
                .contactName(merchant.getContactName())
                .contactEmail(merchant.getContactEmail())
                .contactPhone(merchant.getContactPhone())
                .address(merchant.getAddress())
                .status(merchant.getStatus())
                .config(merchant.getConfig())
                .createdAt(merchant.getCreatedAt())
                .updatedAt(merchant.getUpdatedAt())
                .build();
    }
}
