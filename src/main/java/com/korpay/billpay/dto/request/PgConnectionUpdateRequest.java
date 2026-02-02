package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.PgConnectionStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PgConnectionUpdateRequest {

    @Size(max = 50, message = "PG 이름은 50자를 초과할 수 없습니다")
    private String pgName;

    @Size(max = 20, message = "API 버전은 20자를 초과할 수 없습니다")
    private String pgApiVersion;

    private String apiKey;

    private String apiSecret;

    @Size(max = 100, message = "Webhook Secret은 100자를 초과할 수 없습니다")
    private String webhookSecret;

    @Size(max = 200, message = "API Base URL은 200자를 초과할 수 없습니다")
    private String apiBaseUrl;

    private Map<String, String> apiEndpoints;

    private PgConnectionStatus status;
}
