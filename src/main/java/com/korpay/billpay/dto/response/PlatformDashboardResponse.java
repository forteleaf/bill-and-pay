package com.korpay.billpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformDashboardResponse {

    private long totalTenants;
    private long activeTenants;
    private long suspendedTenants;
    private long provisioningTenants;
    private long totalAuthUsers;
    private long totalPgConnections;
}
