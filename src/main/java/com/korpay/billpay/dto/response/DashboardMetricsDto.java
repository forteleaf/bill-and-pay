package com.korpay.billpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {
    
    private Long todaySales;
    private Long monthSales;
    private Long pendingSettlements;
    private Long transactionCount;
}
