package com.korpay.billpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantStatisticsDto {
    
    private Long totalTransactions;
    private Long totalAmount;
    private Long approvedTransactions;
    private Long approvedAmount;
    private Long cancelledTransactions;
    private Long cancelledAmount;
    private Long pendingTransactions;
    private Long pendingAmount;
}
