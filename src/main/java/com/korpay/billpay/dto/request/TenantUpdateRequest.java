package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantUpdateRequest {

    private String name;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String contactEmail;

    private String contactPhone;
}
