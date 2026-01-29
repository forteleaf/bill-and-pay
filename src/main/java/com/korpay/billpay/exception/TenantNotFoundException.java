package com.korpay.billpay.exception;

/**
 * Exception thrown when a requested tenant cannot be found in the system.
 * 
 * <p>This exception is typically thrown when:
 * <ul>
 *   <li>A client provides an X-Tenant-ID header that doesn't match any registered tenant</li>
 *   <li>An attempt is made to access a tenant schema that doesn't exist</li>
 *   <li>A tenant has been deactivated or removed from the system</li>
 * </ul>
 * 
 * @see InvalidTenantException for format validation errors
 */
public class TenantNotFoundException extends RuntimeException {
    
    private final String tenantId;
    
    /**
     * Constructs a new TenantNotFoundException with the specified tenant ID.
     * 
     * @param tenantId the tenant ID that was not found
     */
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
        this.tenantId = tenantId;
    }
    
    /**
     * Constructs a new TenantNotFoundException with the specified tenant ID and cause.
     * 
     * @param tenantId the tenant ID that was not found
     * @param cause the underlying cause of the exception
     */
    public TenantNotFoundException(String tenantId, Throwable cause) {
        super("Tenant not found: " + tenantId, cause);
        this.tenantId = tenantId;
    }
    
    /**
     * Returns the tenant ID that was not found.
     * 
     * @return the tenant ID
     */
    public String getTenantId() {
        return tenantId;
    }
}
