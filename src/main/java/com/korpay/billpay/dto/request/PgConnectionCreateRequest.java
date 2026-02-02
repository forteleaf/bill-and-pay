package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class PgConnectionCreateRequest {

    @NotBlank(message = "PG 코드는 필수입니다")
    @Size(max = 20, message = "PG 코드는 20자를 초과할 수 없습니다")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "PG 코드는 대문자, 숫자, 언더스코어만 사용 가능합니다")
    private String pgCode;

    @NotBlank(message = "PG 이름은 필수입니다")
    @Size(max = 50, message = "PG 이름은 50자를 초과할 수 없습니다")
    private String pgName;

    @Size(max = 20, message = "API 버전은 20자를 초과할 수 없습니다")
    private String pgApiVersion;

    @NotBlank(message = "가맹점 ID는 필수입니다")
    @Size(max = 100, message = "가맹점 ID는 100자를 초과할 수 없습니다")
    private String merchantId;

    @NotBlank(message = "API Key는 필수입니다")
    private String apiKey;

    @NotBlank(message = "API Secret은 필수입니다")
    private String apiSecret;

    @NotBlank(message = "Webhook 경로는 필수입니다")
    @Size(max = 100, message = "Webhook 경로는 100자를 초과할 수 없습니다")
    @Pattern(regexp = "^/api/webhook/[a-z0-9_-]+$", message = "Webhook 경로는 '/api/webhook/'로 시작해야 합니다")
    private String webhookPath;

    @Size(max = 100, message = "Webhook Secret은 100자를 초과할 수 없습니다")
    private String webhookSecret;

    @Size(max = 200, message = "API Base URL은 200자를 초과할 수 없습니다")
    private String apiBaseUrl;

    private Map<String, String> apiEndpoints;
}
