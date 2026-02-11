package com.korpay.billpay.controller.platform;

import com.korpay.billpay.dto.request.LoginRequest;
import com.korpay.billpay.dto.request.RefreshTokenRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.PlatformAuthResponse;
import com.korpay.billpay.service.platform.PlatformAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/platform/auth")
@RequiredArgsConstructor
public class PlatformAuthController {

    private final PlatformAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<PlatformAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Platform admin login attempt: {}", request.getUsername());
        PlatformAuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<PlatformAuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Platform admin token refresh");
        PlatformAuthResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
