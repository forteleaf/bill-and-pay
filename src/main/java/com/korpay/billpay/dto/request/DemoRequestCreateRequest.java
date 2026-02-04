package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.Email;
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
public class DemoRequestCreateRequest {

    @NotBlank(message = "회사명은 필수입니다")
    @Size(max = 100, message = "회사명은 100자 이하여야 합니다")
    private String companyName;

    @NotBlank(message = "담당자명은 필수입니다")
    @Size(max = 50, message = "담당자명은 50자 이하여야 합니다")
    private String contactName;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    private String email;

    @NotBlank(message = "연락처는 필수입니다")
    @Pattern(regexp = "^[0-9-]{10,13}$", message = "올바른 전화번호 형식이 아닙니다")
    private String phone;

    @Size(max = 50, message = "직책은 50자 이하여야 합니다")
    private String position;

    @Size(max = 20, message = "임직원 수는 20자 이하여야 합니다")
    private String employeeCount;

    @Size(max = 20, message = "월 거래액은 20자 이하여야 합니다")
    private String monthlyVolume;

    @Size(max = 500, message = "문의사항은 500자 이하여야 합니다")
    private String message;
}
