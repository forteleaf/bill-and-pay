package com.korpay.billpay.config.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * 앱 시작 시 모든 active tenant의 스키마를 초기화합니다.
 */
@Component
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class TenantSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantSchemaInitializer.class);

    private final DataSource dataSource;
    private final TenantFlywayConfiguration tenantFlywayConfiguration;

    public TenantSchemaInitializer(DataSource dataSource, TenantFlywayConfiguration tenantFlywayConfiguration) {
        this.dataSource = dataSource;
        this.tenantFlywayConfiguration = tenantFlywayConfiguration;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting tenant schema initialization...");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<String> tenantSchemas = jdbcTemplate.queryForList(
                "SELECT schema_name FROM public.tenants WHERE status = 'ACTIVE' AND deleted_at IS NULL",
                String.class
        );

        if (tenantSchemas.isEmpty()) {
            log.warn("No active tenants found. Skipping tenant schema initialization.");
            return;
        }

        for (String schemaName : tenantSchemas) {
            try {
                log.info("Initializing schema: {}", schemaName);
                tenantFlywayConfiguration.initializeTenantSchema(schemaName);
            } catch (Exception e) {
                log.error("Failed to initialize schema: {}", schemaName, e);
            }
        }

        log.info("Tenant schema initialization completed. Initialized {} tenant(s).", tenantSchemas.size());
    }
}
