package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.FeeType;
import jakarta.validation.constraints.NotNull;
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
public class FeeConfigurationCreateRequest {

    @NotNull(message = "Payment method ID is required")
    private UUID paymentMethodId;

    @NotNull(message = "Fee type is required")
    @Builder.Default
    private FeeType feeType = FeeType.PERCENTAGE;

    private BigDecimal feeRate;

    private Long fixedFee;

    private Long minFee;

    private Long maxFee;

    @Builder.Default
    private Integer priority = 0;

    private OffsetDateTime validFrom;

    private OffsetDateTime validUntil;

    private String reason;
}
