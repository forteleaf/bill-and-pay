package com.korpay.billpay.service.auth;

import com.korpay.billpay.domain.entity.AuthUser;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.repository.AuthUserRepository;
import com.korpay.billpay.repository.OrganizationRepository;
import com.korpay.billpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserContextHolder {

    private final AuthUserRepository authUserRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new EntityNotFoundException("No authenticated user found");
        }
        
        String username = authentication.getName();
        AuthUser authUser = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        
        return createFallbackUser(authUser);
    }
    
    private User createFallbackUser(AuthUser authUser) {
        User fallbackUser = new User();
        fallbackUser.setId(authUser.getId());
        fallbackUser.setUsername(authUser.getUsername());
        fallbackUser.setEmail("temp@example.com");
        fallbackUser.setFullName(authUser.getUsername());
        fallbackUser.setRole("MASTER_ADMIN");
        fallbackUser.setPasswordHash("");
        
        Organization distributorOrg = organizationRepository.findByOrgType(
                com.korpay.billpay.domain.enums.OrganizationType.DISTRIBUTOR
        ).stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No DISTRIBUTOR organization found"));
        
        fallbackUser.setOrganization(distributorOrg);
        fallbackUser.setOrgPath(distributorOrg.getPath());
        
        return fallbackUser;
    }
}
