package com.korpay.billpay.service.platform;

import com.korpay.billpay.dto.response.PlatformDashboardResponse;
import com.korpay.billpay.repository.AuthUserRepository;
import com.korpay.billpay.repository.PgConnectionRepository;
import com.korpay.billpay.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformDashboardService {

    private final TenantRepository tenantRepository;
    private final AuthUserRepository authUserRepository;
    private final PgConnectionRepository pgConnectionRepository;

    @Transactional(readOnly = true)
    public PlatformDashboardResponse getOverview() {
        return PlatformDashboardResponse.builder()
                .totalTenants(tenantRepository.count())
                .activeTenants(tenantRepository.countByStatus("ACTIVE"))
                .suspendedTenants(tenantRepository.countByStatus("SUSPENDED"))
                .provisioningTenants(tenantRepository.countByStatus("PROVISIONING"))
                .totalAuthUsers(authUserRepository.count())
                .totalPgConnections(pgConnectionRepository.count())
                .build();
    }
}
