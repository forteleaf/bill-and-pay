package com.korpay.billpay.config.tenant;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class TenantFlywayConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(TenantFlywayConfiguration.class);
    
    private final TenantDataSourceProperties tenantProperties;
    
    public TenantFlywayConfiguration(TenantDataSourceProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }
    
    public void initializeTenantSchema(String tenantId) {
        String schemaName = tenantId.startsWith("tenant_") ? tenantId : "tenant_" + tenantId;
        
        try {
            DataSource dataSource = new DriverManagerDataSource(
                    tenantProperties.getUrl(),
                    tenantProperties.getUsername(),
                    tenantProperties.getPassword()
            );
            
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schemaName)
                    .locations("classpath:db/migration/tenant")
                    .baselineOnMigrate(true)
                    .load();
            
            flyway.migrate();
            log.info("Flyway migration completed for tenant schema: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to initialize Flyway for tenant schema: {}", schemaName, e);
            throw new RuntimeException("Flyway initialization failed for tenant: " + tenantId, e);
        }
    }
}
