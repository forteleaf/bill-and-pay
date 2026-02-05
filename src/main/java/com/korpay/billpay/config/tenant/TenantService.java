package com.korpay.billpay.config.tenant;

import com.korpay.billpay.exception.TenantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class TenantService {
    
    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    
    private final TenantRoutingDataSource routingDataSource;
    private final DataSource publicDataSource;
    
    public TenantService(TenantRoutingDataSource routingDataSource,
                        @org.springframework.beans.factory.annotation.Qualifier("publicDataSource") DataSource publicDataSource) {
        this.routingDataSource = routingDataSource;
        this.publicDataSource = publicDataSource;
    }

    public boolean tenantExists(String tenantId) {
        return routingDataSource.tenantExistsInDatabase(tenantId);
    }

    public void validateTenantExists(String tenantId) {
        if (!tenantExists(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
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
    
    /**
     * Returns list of all active tenant schema names.
     */
    public List<String> getAllActiveTenants() {
        List<String> tenants = new ArrayList<>();
        String sql = "SELECT schema_name FROM tenants WHERE status = 'ACTIVE'";
        
        try (Connection conn = publicDataSource.getConnection();
             var stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                tenants.add(rs.getString("schema_name"));
            }
            log.debug("Found {} active tenants", tenants.size());
            
        } catch (SQLException e) {
            log.error("Failed to fetch active tenants", e);
        }
        
        return tenants;
    }
}
