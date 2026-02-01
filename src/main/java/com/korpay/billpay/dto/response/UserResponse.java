package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private UUID orgId;
    private String orgPath;
    private String fullName;
    private String phone;
    private String role;
    private List<String> permissions;
    private Boolean twoFactorEnabled;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime passwordChangedAt;
    private UserStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .orgId(user.getOrganization().getId())
                .orgPath(user.getOrgPath())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .permissions(user.getPermissions())
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .passwordChangedAt(user.getPasswordChangedAt())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
