package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDto {
    
    private UUID id;
    private UUID settlementBatchId;
    private UUID transactionEventId;
    private UUID transactionId;
    private UUID merchantId;
    private String merchantPath;
    private UUID entityId;
    private OrganizationType entityType;
    private String entityPath;
    private EntryType entryType;
    private Long amount;
    private Long feeAmount;
    private Long netAmount;
    private String currency;
    private BigDecimal feeRate;
    private Map<String, Object> feeConfig;
    private SettlementStatus status;
    private OffsetDateTime settledAt;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public static SettlementDto from(Settlement settlement) {
        return SettlementDto.builder()
                .id(settlement.getId())
                .settlementBatchId(settlement.getSettlementBatch() != null ? settlement.getSettlementBatch().getId() : null)
                .transactionEventId(settlement.getTransactionEventId())
                .transactionId(settlement.getTransactionId())
                .merchantId(settlement.getMerchantId())
                .merchantPath(settlement.getMerchantPath())
                .entityId(settlement.getEntityId())
                .entityType(settlement.getEntityType())
                .entityPath(settlement.getEntityPath())
                .entryType(settlement.getEntryType())
                .amount(settlement.getAmount())
                .feeAmount(settlement.getFeeAmount())
                .netAmount(settlement.getNetAmount())
                .currency(settlement.getCurrency())
                .feeRate(settlement.getFeeRate())
                .feeConfig(settlement.getFeeConfig())
                .status(settlement.getStatus())
                .settledAt(settlement.getSettledAt())
                .metadata(settlement.getMetadata())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .build();
    }
}
