package com.korpay.billpay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.domain.enums.PgConnectionStatus;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PgConnectionDto {

    private UUID id;
    private String pgCode;
    private String pgName;
    private String pgApiVersion;
    private String merchantId;
    private String webhookPath;
    private String apiBaseUrl;
    private Map<String, String> apiEndpoints;
    private PgConnectionStatus status;
    private OffsetDateTime lastSyncAt;
    private String lastError;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static PgConnectionDto from(PgConnection entity) {
        return PgConnectionDto.builder()
                .id(entity.getId())
                .pgCode(entity.getPgCode())
                .pgName(entity.getPgName())
                .pgApiVersion(entity.getPgApiVersion())
                .merchantId(entity.getMerchantId())
                .webhookPath(entity.getWebhookPath())
                .apiBaseUrl(entity.getApiBaseUrl())
                .apiEndpoints(entity.getApiEndpoints())
                .status(entity.getStatus())
                .lastSyncAt(entity.getLastSyncAt())
                .lastError(entity.getLastError())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
