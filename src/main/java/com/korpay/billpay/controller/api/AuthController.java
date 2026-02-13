package com.korpay.billpay.controller.api;

import com.korpay.billpay.dto.request.LoginRequest;
import com.korpay.billpay.dto.request.RefreshTokenRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.AuthResponse;
import com.korpay.billpay.service.auth.AuthService;
import com.korpay.billpay.service.auth.LoginRateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        String username = request.getUsername();
        log.info("Login attempt for username: {}", username);

        if (loginRateLimiter.isBlocked(username)) {
            long remaining = loginRateLimiter.getLockoutRemainingSeconds(username);
            log.warn("로그인 차단됨: username={}, 남은시간={}초", username, remaining);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.error("RATE_LIMIT_EXCEEDED",
                            "로그인 시도 횟수를 초과했습니다. " + remaining + "초 후에 다시 시도하세요."));
        }

        try {
            AuthResponse authResponse = authService.login(request);
            loginRateLimiter.recordSuccess(username);
            return ResponseEntity.ok(ApiResponse.success(authResponse));
        } catch (Exception e) {
            loginRateLimiter.recordFailure(username);
            throw e;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh attempt");
        
        AuthResponse authResponse = authService.refresh(request);
        
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        log.info("Logout attempt");
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
