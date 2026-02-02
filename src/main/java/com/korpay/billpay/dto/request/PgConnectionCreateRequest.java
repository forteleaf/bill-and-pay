package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Size(max = 100, message = "PG 이름은 100자를 초과할 수 없습니다")
    private String pgName;

    @NotBlank(message = "API Base URL은 필수입니다")
    @Size(max = 500, message = "API Base URL은 500자를 초과할 수 없습니다")
    private String apiBaseUrl;

    @Size(max = 500, message = "Webhook Base URL은 500자를 초과할 수 없습니다")
    private String webhookBaseUrl;

    @NotBlank(message = "가맹점 ID는 필수입니다")
    private String merchantId;

    @NotBlank(message = "API Key는 필수입니다")
    private String apiKey;

    @NotBlank(message = "Secret Key는 필수입니다")
    private String secretKey;

    private String webhookSecret;
    private Integer timeoutMs;
    private Integer retryCount;
}
