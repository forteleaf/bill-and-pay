package com.korpay.billpay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.korpay.billpay.domain.entity.MerchantPgMapping;
import com.korpay.billpay.domain.enums.MerchantPgMappingStatus;
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
public class MerchantPgMappingDto {

    private UUID id;
    private UUID merchantId;
    private String merchantName;
    private String merchantCode;
    private Long pgConnectionId;
    private String pgCode;
    private String pgName;
    private String mid;
    private String terminalId;
    private String catId;
    private Map<String, Object> config;
    private MerchantPgMappingStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MerchantPgMappingDto from(MerchantPgMapping mapping) {
        return MerchantPgMappingDto.builder()
                .id(mapping.getId())
                .merchantId(mapping.getMerchant() != null ? mapping.getMerchant().getId() : null)
                .merchantName(mapping.getMerchant() != null ? mapping.getMerchant().getName() : null)
                .merchantCode(mapping.getMerchant() != null ? mapping.getMerchant().getMerchantCode() : null)
                .pgConnectionId(mapping.getPgConnectionId())
                .mid(mapping.getMid())
                .terminalId(mapping.getTerminalId())
                .catId(mapping.getCatId())
                .config(mapping.getConfig())
                .status(mapping.getStatus())
                .createdAt(mapping.getCreatedAt())
                .updatedAt(mapping.getUpdatedAt())
                .build();
    }

    public static MerchantPgMappingDto from(MerchantPgMapping mapping, String pgCode, String pgName) {
        MerchantPgMappingDto dto = from(mapping);
        dto.setPgCode(pgCode);
        dto.setPgName(pgName);
        return dto;
    }
}
