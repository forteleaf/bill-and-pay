package com.korpay.billpay.service.auth;

import com.korpay.billpay.domain.entity.AuthUser;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.repository.AuthUserRepository;
import com.korpay.billpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextHolder {

    private final AuthUserRepository authUserRepository;
    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new EntityNotFoundException("No authenticated user found");
        }

        String username = authentication.getName();

        authUserRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("AuthUser not found: " + username));

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Tenant user not found: " + username));
    }
}
