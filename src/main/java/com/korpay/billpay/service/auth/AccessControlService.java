package com.korpay.billpay.service.auth;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.exception.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Access Control Service
 * 
 * Validates ltree path-based authorization according to PRD-02 rules:
 * - Superior → Subordinate: Can access all subordinate data
 * - Subordinate → Superior: CANNOT access superior data
 * - Sibling → Sibling: CANNOT access sibling data
 */
@Slf4j
@Service
public class AccessControlService {

    private static final String MASTER_ROLE = "MASTER_ADMIN";

    /**
     * Validates if the user has access to the target organization.
     * 
     * @param user The current authenticated user
     * @param targetPath The target organization path
     * @throws AccessDeniedException if access is denied
     */
    public void validateOrganizationAccess(User user, String targetPath) {
        if (!hasAccessToPath(user, targetPath)) {
            log.warn("Access denied: user={}, userPath={}, targetPath={}", 
                    user.getUsername(), user.getOrgPath(), targetPath);
            throw new AccessDeniedException(
                    "Access denied to organization: " + targetPath);
        }
    }

    /**
     * Validates if the user has access to the target organization.
     * 
     * @param user The current authenticated user
     * @param targetOrganization The target organization
     * @throws AccessDeniedException if access is denied
     */
    public void validateOrganizationAccess(User user, Organization targetOrganization) {
        validateOrganizationAccess(user, targetOrganization.getPath());
    }

    /**
     * Validates if the user has access to the merchant.
     * 
     * @param user The current authenticated user
     * @param merchant The target merchant
     * @throws AccessDeniedException if access is denied
     */
    public void validateMerchantAccess(User user, Merchant merchant) {
        if (!hasAccessToPath(user, merchant.getOrgPath())) {
            log.warn("Access denied: user={}, userPath={}, merchantPath={}", 
                    user.getUsername(), user.getOrgPath(), merchant.getOrgPath());
            throw new AccessDeniedException(
                    "Access denied to merchant: " + merchant.getMerchantCode());
        }
    }

    /**
     * Validates if the user has access to the merchant path.
     * 
     * @param user The current authenticated user
     * @param merchantPath The merchant's organization path
     * @throws AccessDeniedException if access is denied
     */
    public void validateMerchantAccess(User user, String merchantPath) {
        if (!hasAccessToPath(user, merchantPath)) {
            log.warn("Access denied: user={}, userPath={}, merchantPath={}", 
                    user.getUsername(), user.getOrgPath(), merchantPath);
            throw new AccessDeniedException(
                    "Access denied to merchant with path: " + merchantPath);
        }
    }

    /**
     * Checks if the user has access to the target organization.
     * 
     * @param user The current authenticated user
     * @param targetPath The target organization path
     * @return true if access is granted, false otherwise
     */
    public boolean hasAccessToOrganization(User user, String targetPath) {
        return hasAccessToPath(user, targetPath);
    }

    /**
     * Checks if the user has access to the merchant.
     * 
     * @param user The current authenticated user
     * @param merchant The target merchant
     * @return true if access is granted, false otherwise
     */
    public boolean hasAccessToMerchant(User user, Merchant merchant) {
        return hasAccessToPath(user, merchant.getOrgPath());
    }

    /**
     * Checks if the user can create a child organization under the parent path.
     * 
     * @param user The current authenticated user
     * @param parentPath The parent organization path
     * @return true if creation is allowed, false otherwise
     */
    public boolean canCreateOrganization(User user, String parentPath) {
        // Master admin can create any organization
        if (isMasterAdmin(user)) {
            return true;
        }

        // User can only create organizations under their own path
        return parentPath.startsWith(user.getOrgPath());
    }

    /**
     * Validates if the user can create a child organization under the parent.
     * 
     * @param user The current authenticated user
     * @param parentPath The parent organization path
     * @throws AccessDeniedException if creation is not allowed
     */
    public void validateOrganizationCreation(User user, String parentPath) {
        if (!canCreateOrganization(user, parentPath)) {
            log.warn("Organization creation denied: user={}, userPath={}, parentPath={}", 
                    user.getUsername(), user.getOrgPath(), parentPath);
            throw new AccessDeniedException(
                    "Cannot create organization under: " + parentPath);
        }
    }

    /**
     * Checks if the user is a master admin.
     * 
     * @param user The user to check
     * @return true if the user is a master admin, false otherwise
     */
    public boolean isMasterAdmin(User user) {
        return MASTER_ROLE.equals(user.getRole());
    }

    /**
     * Core access control logic using ltree path hierarchy.
     * 
     * Rules:
     * - Master admin: Full access to everything
     * - Regular user: Can only access paths that start with their org path
     * - This implements the descendant check: targetPath <@ userPath
     * 
     * @param user The current authenticated user
     * @param targetPath The target path to check
     * @return true if access is granted, false otherwise
     */
    private boolean hasAccessToPath(User user, String targetPath) {
        // Master admin has access to everything
        if (isMasterAdmin(user)) {
            return true;
        }

        String userOrgPath = user.getOrgPath();
        if (userOrgPath == null || userOrgPath.isEmpty()) {
            return false;
        }

        if (targetPath == null || targetPath.isEmpty()) {
            return false;
        }

        // Check if target path is a descendant of user's path
        // In ltree terms: targetPath <@ userPath means target is descendant
        // In Java string terms: targetPath.startsWith(userPath)
        return targetPath.startsWith(userOrgPath);
    }
}
