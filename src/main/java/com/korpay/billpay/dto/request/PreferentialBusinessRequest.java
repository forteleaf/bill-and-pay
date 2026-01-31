package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PreferentialBusinessRequest {
    @NotBlank(message = "사업자번호는 필수입니다")
    private String businessNumbers; // comma-separated, max 10
}
