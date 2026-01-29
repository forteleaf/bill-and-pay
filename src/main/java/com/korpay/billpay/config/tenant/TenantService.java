package com.korpay.billpay.config.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TenantService {
    
    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    
    private final TenantRoutingDataSource routingDataSource;
    
    public TenantService(TenantRoutingDataSource routingDataSource) {
        this.routingDataSource = routingDataSource;
    }
    
    public void evictTenantCache(String tenantId) {
        routingDataSource.evictTenantCache(tenantId);
        log.info("Evicted cache for tenant: {}", tenantId);
    }
    
    public void removeTenantDataSource(String tenantId) {
        routingDataSource.removeTenantDataSource(tenantId);
        log.info("Removed datasource for tenant: {}", tenantId);
    }
    
    public void clearAllTenantCaches() {
        routingDataSource.clearAllTenantCaches();
        log.info("Cleared all tenant caches");
    }
    
    public int getActiveTenantDataSourceCount() {
        return routingDataSource.getTenantDataSourceCount();
    }
}
