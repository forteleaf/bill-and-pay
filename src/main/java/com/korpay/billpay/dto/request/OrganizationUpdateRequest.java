package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.OrganizationStatus;
import jakarta.validation.constraints.Email;
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
public class OrganizationUpdateRequest {
    
    private UUID parentId;
    private String name;
    private UUID businessEntityId;
    
    @Email(message = "Invalid email format")
    private String email;
    
    private String phone;
    private String address;
    private OrganizationStatus status;
    private Map<String, Object> config;
}
