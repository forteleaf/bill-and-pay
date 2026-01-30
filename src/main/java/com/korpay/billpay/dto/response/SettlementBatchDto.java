package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.SettlementBatch;
import com.korpay.billpay.domain.enums.SettlementBatchStatus;
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
public class SettlementBatchDto {
    
    private UUID id;
    private String batchNumber;
    private LocalDate settlementDate;
    private SettlementBatchStatus status;
    private Integer totalTransactions;
    private Long totalAmount;
    private Long totalFeeAmount;
    private OffsetDateTime processedAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public static SettlementBatchDto from(SettlementBatch batch) {
        return SettlementBatchDto.builder()
                .id(batch.getId())
                .batchNumber(batch.getBatchNumber())
                .settlementDate(batch.getSettlementDate())
                .status(batch.getStatus())
                .totalTransactions(batch.getTotalTransactions())
                .totalAmount(batch.getTotalAmount())
                .totalFeeAmount(batch.getTotalFeeAmount())
                .processedAt(batch.getProcessedAt())
                .approvedAt(batch.getApprovedAt())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}
