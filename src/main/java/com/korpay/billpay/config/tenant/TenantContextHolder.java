package com.korpay.billpay.config.tenant;

import com.korpay.billpay.exception.InvalidTenantException;

import java.lang.ScopedValue;
import java.util.function.Supplier;

/**
 * Hybrid tenant context holder using ThreadLocal with ScopedValue backup.
 * ThreadLocal is required for Spring AOP transaction management compatibility.
 */
public final class TenantContextHolder {
    
    private static final ScopedValue<String> SCOPED_TENANT_ID = ScopedValue.newInstance();
    private static final ThreadLocal<String> THREAD_LOCAL_TENANT_ID = new ThreadLocal<>();
    
    private static final String TENANT_ID_PATTERN = "^[a-z][a-z0-9_]{2,49}$";
    
    private TenantContextHolder() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
    
    public static String getCurrentTenant() {
        String tenantId = THREAD_LOCAL_TENANT_ID.get();
        if (tenantId != null) {
            return tenantId;
        }
        return SCOPED_TENANT_ID.isBound() ? SCOPED_TENANT_ID.get() : null;
    }
    
    public static <R> R runInTenant(String tenantId, Supplier<R> operation) {
        validateTenantId(tenantId);
        THREAD_LOCAL_TENANT_ID.set(tenantId);
        try {
            return ScopedValue.where(SCOPED_TENANT_ID, tenantId).call(operation::get);
        } finally {
            THREAD_LOCAL_TENANT_ID.remove();
        }
    }
    
    public static <R, X extends Throwable> R runInTenant(String tenantId, ScopedValue.CallableOp<R, X> operation) throws X {
        validateTenantId(tenantId);
        THREAD_LOCAL_TENANT_ID.set(tenantId);
        try {
            return ScopedValue.where(SCOPED_TENANT_ID, tenantId).call(operation);
        } finally {
            THREAD_LOCAL_TENANT_ID.remove();
        }
    }
    
    public static void runInTenant(String tenantId, Runnable operation) {
        validateTenantId(tenantId);
        THREAD_LOCAL_TENANT_ID.set(tenantId);
        try {
            ScopedValue.where(SCOPED_TENANT_ID, tenantId).run(operation);
        } finally {
            THREAD_LOCAL_TENANT_ID.remove();
        }
    }
    
    private static void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidTenantException(tenantId, "Tenant ID cannot be null or blank");
        }
        
        if (!tenantId.matches(TENANT_ID_PATTERN)) {
            throw new InvalidTenantException(tenantId);
        }
    }
    
    public static boolean hasTenantContext() {
        return THREAD_LOCAL_TENANT_ID.get() != null || SCOPED_TENANT_ID.isBound();
    }
}
