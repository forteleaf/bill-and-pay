package com.korpay.billpay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.domain.enums.PgConnectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PgConnectionDto {

    private Long id;
    private String pgCode;
    private String pgName;
    private String merchantId;
    private String apiBaseUrl;
    private String webhookBaseUrl;
    private String tenantId;
    private PgConnectionStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private String webhookUrl;

    public static PgConnectionDto from(PgConnection entity) {
        return PgConnectionDto.builder()
                .id(entity.getId())
                .pgCode(entity.getPgCode())
                .pgName(entity.getPgName())
                .merchantId(entity.getMerchantId())
                .apiBaseUrl(entity.getApiBaseUrl())
                .webhookBaseUrl(entity.getWebhookBaseUrl())
                .tenantId(entity.getTenantId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static PgConnectionDto from(PgConnection entity, String webhookUrl) {
        return PgConnectionDto.builder()
                .id(entity.getId())
                .pgCode(entity.getPgCode())
                .pgName(entity.getPgName())
                .merchantId(entity.getMerchantId())
                .apiBaseUrl(entity.getApiBaseUrl())
                .webhookBaseUrl(entity.getWebhookBaseUrl())
                .tenantId(entity.getTenantId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .webhookUrl(webhookUrl)
                .build();
    }
}
