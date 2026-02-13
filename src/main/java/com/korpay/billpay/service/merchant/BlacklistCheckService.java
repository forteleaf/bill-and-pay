package com.korpay.billpay.service.merchant;

public interface BlacklistCheckService {
    boolean isBlacklisted(String businessNumber);
    String getReason(String businessNumber);
}
