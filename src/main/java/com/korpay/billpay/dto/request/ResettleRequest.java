package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResettleRequest {

    @NotNull(message = "transactionEventId는 필수입니다")
    private UUID transactionEventId;
}
