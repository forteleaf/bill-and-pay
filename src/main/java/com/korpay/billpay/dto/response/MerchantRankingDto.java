package com.korpay.billpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRankingDto {
    
    private UUID merchantId;
    private String merchantName;
    private Long totalAmount;
    private Long transactionCount;
}
