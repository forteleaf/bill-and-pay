package com.korpay.billpay.config.tenant;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;

@Configuration
public class MultiTenantConfiguration implements WebMvcConfigurer {
    
    private final TenantInterceptor tenantInterceptor;
    private final TenantDataSourceProperties tenantProperties;
    
    public MultiTenantConfiguration(TenantInterceptor tenantInterceptor, 
                                   TenantDataSourceProperties tenantProperties) {
        this.tenantInterceptor = tenantInterceptor;
        this.tenantProperties = tenantProperties;
    }
    
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.public")
    public DataSource publicDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }
    
    @Bean
    public HikariConfig tenantDataSourceTemplate() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(tenantProperties.getUrl());
        config.setUsername(tenantProperties.getUsername());
        config.setPassword(tenantProperties.getPassword());
        config.setDriverClassName(tenantProperties.getDriverClassName());
        
        config.setMaximumPoolSize(tenantProperties.getMaximumPoolSize());
        config.setMinimumIdle(tenantProperties.getMinimumIdle());
        config.setConnectionTimeout(tenantProperties.getConnectionTimeout());
        config.setIdleTimeout(tenantProperties.getIdleTimeout());
        config.setMaxLifetime(tenantProperties.getMaxLifetime());
        
        return config;
    }
    
    @Bean
    public TenantRoutingDataSource tenantRoutingDataSource(@Qualifier("publicDataSource") DataSource publicDataSource,
                                                            HikariConfig tenantDataSourceTemplate) {
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(
                publicDataSource, 
                tenantDataSourceTemplate
        );
        routingDataSource.setDefaultTargetDataSource(publicDataSource);
        routingDataSource.setTargetDataSources(new java.util.HashMap<>());
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
    
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource(TenantRoutingDataSource tenantRoutingDataSource) {
        return tenantRoutingDataSource;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/actuator/**",
                        "/api/public/**"
                );
    }
}
