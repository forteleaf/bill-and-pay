package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.OrganizationType;
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
public class OrganizationCreateRequest {
    
    private String orgCode;
    
    @NotBlank(message = "Organization name is required")
    private String name;
    
    @NotNull(message = "Organization type is required")
    private OrganizationType orgType;
    
    private UUID parentId;
    
    private String businessNumber;
    private String businessName;
    private String representativeName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    private String address;
    private Map<String, Object> config;
}
