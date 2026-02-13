package com.korpay.billpay.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long ATTEMPT_WINDOW_MINUTES = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final ConcurrentHashMap<String, LoginAttempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null) {
            return false;
        }
        if (attempt.lockedUntil != null && Instant.now().isBefore(attempt.lockedUntil)) {
            return true;
        }
        if (attempt.lockedUntil != null && Instant.now().isAfter(attempt.lockedUntil)) {
            attempts.remove(username);
            return false;
        }
        return false;
    }

    public long getLockoutRemainingSeconds(String username) {
        LoginAttempt attempt = attempts.get(username);
        if (attempt == null || attempt.lockedUntil == null) {
            return 0;
        }
        long remaining = attempt.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    public void recordFailure(String username) {
        attempts.compute(username, (key, existing) -> {
            if (existing == null) {
                return new LoginAttempt(1, Instant.now(), null);
            }
            if (existing.firstAttempt.plusSeconds(ATTEMPT_WINDOW_MINUTES * 60).isBefore(Instant.now())) {
                return new LoginAttempt(1, Instant.now(), null);
            }
            int newCount = existing.failureCount + 1;
            Instant lockedUntil = null;
            if (newCount >= MAX_ATTEMPTS) {
                lockedUntil = Instant.now().plusSeconds(LOCKOUT_MINUTES * 60);
                log.warn("계정 잠금: username={}, {}분간 로그인 차단", username, LOCKOUT_MINUTES);
            }
            return new LoginAttempt(newCount, existing.firstAttempt, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    @Scheduled(fixedRate = 300_000)
    public void cleanup() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(entry -> {
            LoginAttempt attempt = entry.getValue();
            if (attempt.lockedUntil != null) {
                return now.isAfter(attempt.lockedUntil);
            }
            return attempt.firstAttempt.plusSeconds(ATTEMPT_WINDOW_MINUTES * 60).isBefore(now);
        });
    }

    private record LoginAttempt(int failureCount, Instant firstAttempt, Instant lockedUntil) {}
}
