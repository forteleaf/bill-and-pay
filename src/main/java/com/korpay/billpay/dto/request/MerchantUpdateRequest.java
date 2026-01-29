package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.MerchantStatus;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantUpdateRequest {
    
    private String name;
    private String businessNumber;
    private String businessType;
    private String contactName;
    
    @Email(message = "Invalid email format")
    private String contactEmail;
    
    private String contactPhone;
    private String address;
    private MerchantStatus status;
    private Map<String, Object> config;
}
