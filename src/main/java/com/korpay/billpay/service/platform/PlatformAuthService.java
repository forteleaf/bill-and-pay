package com.korpay.billpay.service.platform;

import com.korpay.billpay.domain.entity.PlatformAdmin;
import com.korpay.billpay.domain.enums.PlatformAdminStatus;
import com.korpay.billpay.dto.request.LoginRequest;
import com.korpay.billpay.dto.request.RefreshTokenRequest;
import com.korpay.billpay.dto.response.PlatformAuthResponse;
import com.korpay.billpay.repository.PlatformAdminRepository;
import com.korpay.billpay.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformAuthService {

    private final PlatformAdminRepository adminRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public PlatformAuthResponse login(LoginRequest request) {
        PlatformAdmin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("잘못된 관리자 계정입니다."));

        if (admin.getStatus() != PlatformAdminStatus.ACTIVE) {
            throw new RuntimeException("비활성화된 관리자 계정입니다.");
        }

        if (admin.isLocked()) {
            throw new RuntimeException("계정이 잠겨 있습니다. 30분 후에 다시 시도하세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            admin.recordLoginFailure();
            adminRepository.save(admin);
            throw new RuntimeException("잘못된 관리자 계정입니다.");
        }

        admin.recordLoginSuccess();
        adminRepository.save(admin);

        String accessToken = tokenProvider.generatePlatformAccessToken(
                admin.getUsername(), admin.getRole().name(), admin.getId().toString());
        String refreshToken = tokenProvider.generatePlatformRefreshToken(
                admin.getUsername(), admin.getRole().name(), admin.getId().toString());

        log.info("Platform admin login success: {} (role: {})", admin.getUsername(), admin.getRole());

        return PlatformAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(admin.getUsername())
                .role(admin.getRole().name())
                .fullName(admin.getFullName())
                .build();
    }

    @Transactional(readOnly = true)
    public PlatformAuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!JwtTokenProvider.TOKEN_TYPE_PLATFORM.equals(tokenType)) {
            throw new RuntimeException("플랫폼 관리자 토큰이 아닙니다.");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        PlatformAdmin admin = adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다."));

        String newAccessToken = tokenProvider.generatePlatformAccessToken(
                admin.getUsername(), admin.getRole().name(), admin.getId().toString());
        String newRefreshToken = tokenProvider.generatePlatformRefreshToken(
                admin.getUsername(), admin.getRole().name(), admin.getId().toString());

        return PlatformAuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(admin.getUsername())
                .role(admin.getRole().name())
                .fullName(admin.getFullName())
                .build();
    }
}
