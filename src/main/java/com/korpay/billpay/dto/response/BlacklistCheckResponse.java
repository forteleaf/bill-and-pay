package com.korpay.billpay.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistCheckResponse {
    
    private boolean isBlacklisted;
    private String reason;
    private OffsetDateTime checkedAt;
}
