package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.FeeConfigStatus;
import com.korpay.billpay.domain.enums.FeeType;
import jakarta.validation.constraints.NotBlank;
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
public class FeeConfigurationUpdateRequest {

    private UUID paymentMethodId;

    private FeeType feeType;

    private BigDecimal feeRate;

    private Long fixedFee;

    private Long minFee;

    private Long maxFee;

    private Integer priority;

    private OffsetDateTime validFrom;

    private OffsetDateTime validUntil;

    private FeeConfigStatus status;

    @NotBlank(message = "Reason is required for updates")
    private String reason;
}
