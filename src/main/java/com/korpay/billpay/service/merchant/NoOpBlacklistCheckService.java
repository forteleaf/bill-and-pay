package com.korpay.billpay.service.merchant;

import org.springframework.stereotype.Component;

@Component
public class NoOpBlacklistCheckService implements BlacklistCheckService {

    @Override
    public boolean isBlacklisted(String businessNumber) {
        return false;
    }

    @Override
    public String getReason(String businessNumber) {
        return null;
    }
}
