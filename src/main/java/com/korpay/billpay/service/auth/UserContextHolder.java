package com.korpay.billpay.service.auth;

import com.korpay.billpay.domain.entity.AuthUser;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserContextHolder {

    private final AuthUserRepository authUserRepository;
    
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new EntityNotFoundException("No authenticated user found");
        }
        
        String username = authentication.getName();
        AuthUser authUser = authUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
        
        User user = new User();
        user.setId(authUser.getId());
        user.setUsername(authUser.getUsername());
        user.setEmail("temp@example.com");
        user.setFullName(authUser.getUsername());
        user.setRole("MASTER_ADMIN");
        user.setPasswordHash("");
        user.setOrgPath("");
        
        return user;
    }
}
