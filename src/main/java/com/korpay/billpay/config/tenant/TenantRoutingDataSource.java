package com.korpay.billpay.config.tenant;

import com.korpay.billpay.exception.TenantNotFoundException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {
    
    private static final Logger log = LoggerFactory.getLogger(TenantRoutingDataSource.class);
    
    private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();
    private final HikariConfig tenantDataSourceTemplate;
    private final DataSource publicDataSource;
    private final Map<String, Boolean> tenantExistenceCache = new ConcurrentHashMap<>();
    
    public TenantRoutingDataSource(DataSource publicDataSource, HikariConfig tenantDataSourceTemplate) {
        this.publicDataSource = publicDataSource;
        this.tenantDataSourceTemplate = tenantDataSourceTemplate;
    }
    
    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContextHolder.getCurrentTenant();
        
        if (tenantId == null) {
            return "public";
        }
        
        if (!tenantExistsInDatabase(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
        
        return tenantId;
    }
    
    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();
        
        if ("public".equals(lookupKey)) {
            return publicDataSource;
        }
        
        String tenantId = (String) lookupKey;
        return tenantDataSources.computeIfAbsent(tenantId, this::createTenantDataSource);
    }
    
    private DataSource createTenantDataSource(String tenantId) {
        log.info("Creating new DataSource for tenant: {}", tenantId);
        
        String schemaName = tenantId.startsWith("tenant_") ? tenantId : "tenant_" + tenantId;
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(tenantDataSourceTemplate.getJdbcUrl());
        config.setUsername(tenantDataSourceTemplate.getUsername());
        config.setPassword(tenantDataSourceTemplate.getPassword());
        config.setDriverClassName(tenantDataSourceTemplate.getDriverClassName());
        
        config.setSchema(schemaName);
        config.setPoolName("HikariPool-" + schemaName);
        
        // Set search_path to include both tenant schema and public (for ltree operators)
        config.setConnectionInitSql("SET search_path TO " + schemaName + ", public");
        
        config.setMaximumPoolSize(tenantDataSourceTemplate.getMaximumPoolSize());
        config.setMinimumIdle(tenantDataSourceTemplate.getMinimumIdle());
        config.setConnectionTimeout(tenantDataSourceTemplate.getConnectionTimeout());
        config.setIdleTimeout(tenantDataSourceTemplate.getIdleTimeout());
        config.setMaxLifetime(tenantDataSourceTemplate.getMaxLifetime());
        
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        return new HikariDataSource(config);
    }
    
    private boolean tenantExistsInDatabase(String tenantId) {
        return tenantExistenceCache.computeIfAbsent(tenantId, tid -> {
            String sql = "SELECT EXISTS(SELECT 1 FROM tenants WHERE schema_name = ? AND status = 'ACTIVE')";
            
            try (Connection conn = publicDataSource.getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, tid);
                var rs = stmt.executeQuery();
                
                if (rs.next()) {
                    boolean exists = rs.getBoolean(1);
                    log.debug("Tenant {} existence check: {}", tid, exists);
                    return exists;
                }
                
                return false;
            } catch (SQLException e) {
                log.error("Failed to check tenant existence for: {}", tid, e);
                return false;
            }
        });
    }
    
    public void evictTenantCache(String tenantId) {
        tenantExistenceCache.remove(tenantId);
        log.debug("Evicted tenant cache for: {}", tenantId);
    }
    
    public void removeTenantDataSource(String tenantId) {
        DataSource dataSource = tenantDataSources.remove(tenantId);
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            hikariDataSource.close();
            log.info("Closed and removed DataSource for tenant: {}", tenantId);
        }
        evictTenantCache(tenantId);
    }
    
    public void clearAllTenantCaches() {
        tenantExistenceCache.clear();
        log.info("Cleared all tenant existence caches");
    }
    
    public int getTenantDataSourceCount() {
        return tenantDataSources.size();
    }
}
