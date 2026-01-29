package com.korpay.billpay.dto.settlement;

import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class SettlementSummary {
    private UUID entityId;
    private OrganizationType entityType;
    private String entityPath;
    private EntryType entryType;
    private Long totalAmount;
    private Long feeAmount;
    private Long netAmount;
    private Integer settlementCount;
    private OffsetDateTime periodStart;
    private OffsetDateTime periodEnd;
}
