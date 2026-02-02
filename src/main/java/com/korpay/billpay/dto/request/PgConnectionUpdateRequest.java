package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.PgConnectionStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgConnectionUpdateRequest {

    @Size(max = 100, message = "PG 이름은 100자를 초과할 수 없습니다")
    private String pgName;

    @Size(max = 500, message = "API Base URL은 500자를 초과할 수 없습니다")
    private String apiBaseUrl;

    @Size(max = 500, message = "Webhook Base URL은 500자를 초과할 수 없습니다")
    private String webhookBaseUrl;

    private String merchantId;
    private String apiKey;
    private String secretKey;
    private String webhookSecret;
    private Integer timeoutMs;
    private Integer retryCount;
    private PgConnectionStatus status;
}
