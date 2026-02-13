package com.korpay.billpay.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("prod")
@Component
public class JwtSecurityValidator {

    private static final String DEFAULT_SECRET =
            "billpay-secret-key-for-jwt-token-generation-minimum-256-bits-required-for-hs256-algorithm";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @PostConstruct
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.equals(DEFAULT_SECRET)) {
            throw new IllegalStateException(
                    "운영 환경에서 기본 JWT 시크릿을 사용할 수 없습니다. " +
                    "JWT_SECRET 환경변수를 설정하세요."
            );
        }
    }
}
