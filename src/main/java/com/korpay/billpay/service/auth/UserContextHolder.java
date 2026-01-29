package com.korpay.billpay.service.auth;

import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserContextHolder {

    private final UserRepository userRepository;
    
    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();
    
    public void setCurrentUserId(UUID userId) {
        currentUserId.set(userId);
    }
    
    public void clear() {
        currentUserId.remove();
    }
    
    public User getCurrentUser() {
        UUID userId = currentUserId.get();
        if (userId == null) {
            throw new EntityNotFoundException("No authenticated user found");
        }
        
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }
}
