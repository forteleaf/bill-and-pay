package com.korpay.billpay.exception;

/**
 * Exception thrown when a tenant ID has an invalid format or contains illegal characters.
 * 
 * <p>This exception is used for security validation to prevent SQL injection attacks
 * through tenant identifiers. Valid tenant IDs must:
 * <ul>
 *   <li>Contain only lowercase alphanumeric characters and underscores</li>
 *   <li>Start with a letter</li>
 *   <li>Be between 3 and 50 characters in length</li>
 *   <li>Match the pattern: ^[a-z][a-z0-9_]{2,49}$</li>
 * </ul>
 * 
 * <p>Examples:
 * <ul>
 *   <li>Valid: "tenant_001", "company_abc", "demo123"</li>
 *   <li>Invalid: "Tenant-001" (uppercase/hyphen), "123tenant" (starts with number), 
 *       "tenant; DROP TABLE" (SQL injection attempt)</li>
 * </ul>
 * 
 * @see TenantNotFoundException for tenant existence errors
 */
public class InvalidTenantException extends RuntimeException {
    
    private final String tenantId;
    private final String reason;
    
    /**
     * Constructs a new InvalidTenantException with the specified tenant ID and reason.
     * 
     * @param tenantId the invalid tenant ID
     * @param reason a description of why the tenant ID is invalid
     */
    public InvalidTenantException(String tenantId, String reason) {
        super(String.format("Invalid tenant ID '%s': %s", tenantId, reason));
        this.tenantId = tenantId;
        this.reason = reason;
    }
    
    /**
     * Constructs a new InvalidTenantException with the specified tenant ID.
     * 
     * @param tenantId the invalid tenant ID
     */
    public InvalidTenantException(String tenantId) {
        this(tenantId, "Tenant ID must contain only lowercase alphanumeric characters and underscores, " +
                       "start with a letter, and be 3-50 characters long");
    }
    
    /**
     * Returns the invalid tenant ID.
     * 
     * @return the tenant ID
     */
    public String getTenantId() {
        return tenantId;
    }
    
    /**
     * Returns the reason why the tenant ID is invalid.
     * 
     * @return the validation failure reason
     */
    public String getReason() {
        return reason;
    }
}
