package com.korpay.billpay.config.tenant;

import com.korpay.billpay.exception.InvalidTenantException;

import java.lang.ScopedValue;
import java.util.function.Supplier;

/**
 * Thread-safe tenant context holder using Java 25 ScopedValue.
 * 
 * <p>This class manages the current tenant context for multi-tenant operations.
 * Unlike ThreadLocal, ScopedValue provides structured concurrency guarantees and
 * ensures tenant context is properly scoped to specific execution blocks.
 * 
 * <p><b>Usage Pattern:</b>
 * <pre>{@code
 * // Execute operation in tenant context
 * String result = TenantContextHolder.runInTenant("tenant_001", () -> {
 *     // All database operations here use tenant_001 schema
 *     return service.processData();
 * });
 * 
 * // Get current tenant (only within runInTenant scope)
 * String tenantId = TenantContextHolder.getCurrentTenant();
 * }</pre>
 * 
 * <p><b>Security:</b> Tenant IDs are validated using strict pattern matching to
 * prevent SQL injection attacks through schema name manipulation.
 * 
 * @see ScopedValue
 */
public final class TenantContextHolder {
    
    private static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();
    
    private static final String TENANT_ID_PATTERN = "^[a-z][a-z0-9_]{2,49}$";
    
    private TenantContextHolder() {
        throw new AssertionError("Utility class cannot be instantiated");
    }
    
    /**
     * Returns the current tenant ID from the scoped context.
     *
     * @return the current tenant ID, or null if no tenant context is active
     */
    public static String getCurrentTenant() {
        return TENANT_ID.isBound() ? TENANT_ID.get() : null;
    }
    
    /**
     * Executes a supplier within a tenant context scope.
     *
     * @param tenantId the tenant ID to set for this execution scope
     * @param operation the operation to execute with tenant context
     * @param <R> the return type
     * @return the result of the operation
     * @throws InvalidTenantException if tenant ID format is invalid
     */
    public static <R> R runInTenant(String tenantId, Supplier<R> operation) {
        validateTenantId(tenantId);
        return ScopedValue.where(TENANT_ID, tenantId).call(operation::get);
    }
    
    /**
     * Executes a CallableOp within a tenant context scope.
     *
     * @param tenantId the tenant ID to set for this execution scope
     * @param operation the operation to execute with tenant context
     * @param <R> the return type
     * @param <X> the exception type that may be thrown
     * @return the result of the operation
     * @throws InvalidTenantException if tenant ID format is invalid
     * @throws X if the operation throws an exception
     */
    public static <R, X extends Throwable> R runInTenant(String tenantId, ScopedValue.CallableOp<R, X> operation) throws X {
        validateTenantId(tenantId);
        return ScopedValue.where(TENANT_ID, tenantId).call(operation);
    }
    
    /**
     * Executes a runnable within a tenant context scope.
     * 
     * @param tenantId the tenant ID to set for this execution scope
     * @param operation the runnable to execute with tenant context
     * @throws InvalidTenantException if tenant ID format is invalid
     */
    public static void runInTenant(String tenantId, Runnable operation) {
        validateTenantId(tenantId);
        ScopedValue.where(TENANT_ID, tenantId).run(operation);
    }
    
    /**
     * Validates tenant ID format to prevent SQL injection.
     * Tenant IDs must: start with lowercase letter, contain only lowercase
     * alphanumeric and underscores, be 3-50 characters long.
     * 
     * @param tenantId the tenant ID to validate
     * @throws InvalidTenantException if validation fails
     */
    private static void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidTenantException(tenantId, "Tenant ID cannot be null or blank");
        }
        
        if (!tenantId.matches(TENANT_ID_PATTERN)) {
            throw new InvalidTenantException(tenantId);
        }
    }
    
    /**
     * Checks if a tenant context is currently active.
     * 
     * @return true if within a tenant context scope, false otherwise
     */
    public static boolean hasTenantContext() {
        return TENANT_ID.isBound();
    }
}
