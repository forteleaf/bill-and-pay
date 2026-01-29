package com.korpay.billpay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantCreateRequest {
    
    @NotBlank(message = "Merchant code is required")
    private String merchantCode;
    
    @NotBlank(message = "Merchant name is required")
    private String name;
    
    @NotNull(message = "Organization ID is required")
    private UUID organizationId;
    
    private String businessNumber;
    private String businessType;
    private String contactName;
    
    @Email(message = "Invalid email format")
    private String contactEmail;
    
    private String contactPhone;
    private String address;
    private Map<String, Object> config;
}
