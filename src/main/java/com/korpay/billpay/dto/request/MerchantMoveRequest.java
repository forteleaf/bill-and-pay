package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantMoveRequest {

    @NotNull(message = "Target organization ID is required")
    private UUID targetOrgId;

    private String reason;
}
