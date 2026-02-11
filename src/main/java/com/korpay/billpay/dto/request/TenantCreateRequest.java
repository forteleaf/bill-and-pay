package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantCreateRequest {

    @NotBlank(message = "테넌트 ID는 필수입니다")
    @Pattern(regexp = "^[a-z0-9_]{3,30}$", message = "테넌트 ID는 영소문자, 숫자, 언더스코어만 가능합니다 (3~30자)")
    private String tenantId;

    @NotBlank(message = "테넌트 이름은 필수입니다")
    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String contactEmail;

    private String contactPhone;

    @NotBlank(message = "관리자 사용자명은 필수입니다")
    private String adminUsername;

    @NotBlank(message = "관리자 초기 비밀번호는 필수입니다")
    private String adminPassword;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String adminEmail;
}
