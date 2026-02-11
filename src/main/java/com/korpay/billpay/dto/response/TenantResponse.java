package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.Tenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {

    private String id;
    private String name;
    private String schemaName;
    private String status;
    private String contactEmail;
    private String contactPhone;
    private Map<String, Object> config;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TenantResponse from(Tenant tenant) {
        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .schemaName(tenant.getSchemaName())
                .status(tenant.getStatus())
                .contactEmail(tenant.getContactEmail())
                .contactPhone(tenant.getContactPhone())
                .config(tenant.getConfig())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
