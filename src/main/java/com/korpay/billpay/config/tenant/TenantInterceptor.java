package com.korpay.billpay.config.tenant;

import com.korpay.billpay.exception.InvalidTenantException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Order(1)
public class TenantInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-ID";
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, 
                            @NonNull HttpServletResponse response, 
                            @NonNull Object handler) {
        
        String tenantId = (String) request.getAttribute("tenantId");
        
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getHeader(TENANT_HEADER);
        }
        
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Missing {} header and no tenant from JWT for request: {} {}", 
                    TENANT_HEADER, request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        
        try {
            request.setAttribute("tenantId", tenantId);
            log.debug("Tenant context set: {} for {} {}", 
                    tenantId, request.getMethod(), request.getRequestURI());
            return true;
        } catch (InvalidTenantException e) {
            log.warn("Invalid tenant ID format: {}", tenantId, e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
    }
    
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                               @NonNull HttpServletResponse response,
                               @NonNull Object handler,
                               Exception ex) {
        String tenantId = (String) request.getAttribute("tenantId");
        if (tenantId != null) {
            log.debug("Request completed for tenant: {}", tenantId);
        }
    }
}
