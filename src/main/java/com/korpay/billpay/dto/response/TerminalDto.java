package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.Terminal;
import com.korpay.billpay.domain.enums.TerminalStatus;
import com.korpay.billpay.domain.enums.TerminalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalDto {

    private UUID id;
    private String catId;
    private TerminalType terminalType;
    private UUID merchantId;
    private String merchantName;
    private String merchantCode;
    private UUID organizationId;
    private String organizationName;
    private String serialNumber;
    private String model;
    private String manufacturer;
    private String installAddress;
    private LocalDate installDate;
    private TerminalStatus status;
    private Map<String, Object> config;
    private OffsetDateTime lastTransactionAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static TerminalDto from(Terminal terminal) {
        return TerminalDto.builder()
                .id(terminal.getId())
                .catId(terminal.getCatId())
                .terminalType(terminal.getTerminalType())
                .merchantId(terminal.getMerchant().getId())
                .merchantName(terminal.getMerchant().getName())
                .merchantCode(terminal.getMerchant().getMerchantCode())
                .organizationId(terminal.getOrganization() != null ? terminal.getOrganization().getId() : null)
                .organizationName(terminal.getOrganization() != null ? terminal.getOrganization().getName() : null)
                .serialNumber(terminal.getSerialNumber())
                .model(terminal.getModel())
                .manufacturer(terminal.getManufacturer())
                .installAddress(terminal.getInstallAddress())
                .installDate(terminal.getInstallDate())
                .status(terminal.getStatus())
                .config(terminal.getConfig())
                .lastTransactionAt(terminal.getLastTransactionAt())
                .createdAt(terminal.getCreatedAt())
                .updatedAt(terminal.getUpdatedAt())
                .build();
    }
}
