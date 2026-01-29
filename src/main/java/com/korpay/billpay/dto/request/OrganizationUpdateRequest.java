package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.OrganizationStatus;
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
public class OrganizationUpdateRequest {
    
    private String name;
    private String businessNumber;
    private String businessName;
    private String representativeName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    private String address;
    private OrganizationStatus status;
    private Map<String, Object> config;
}
