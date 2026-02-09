package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.FeeConfigHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeConfigHistoryResponse {

    private UUID id;
    private UUID feeConfigurationId;
    private UUID merchantId;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private BigDecimal oldFeeRate;
    private BigDecimal newFeeRate;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private String changedBy;
    private OffsetDateTime changedAt;
    private OffsetDateTime createdAt;

    public static FeeConfigHistoryResponse from(FeeConfigHistory h) {
        return FeeConfigHistoryResponse.builder()
                .id(h.getId())
                .feeConfigurationId(h.getFeeConfigurationId())
                .merchantId(h.getMerchantId())
                .action(h.getAction())
                .fieldName(h.getFieldName())
                .oldValue(h.getOldValue())
                .newValue(h.getNewValue())
                .oldFeeRate(h.getOldFeeRate())
                .newFeeRate(h.getNewFeeRate())
                .oldStatus(h.getOldStatus())
                .newStatus(h.getNewStatus())
                .reason(h.getReason())
                .changedBy(h.getChangedBy())
                .changedAt(h.getChangedAt())
                .createdAt(h.getCreatedAt())
                .build();
    }
}
