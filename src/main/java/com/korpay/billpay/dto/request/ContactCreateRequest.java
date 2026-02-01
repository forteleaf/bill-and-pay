package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.domain.enums.ContactRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactCreateRequest {

    @NotBlank(message = "담당자 이름은 필수입니다")
    @Size(max = 100, message = "담당자 이름은 100자 이하여야 합니다")
    private String name;

    @Size(max = 20, message = "연락처는 20자 이하여야 합니다")
    private String phone;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다")
    private String email;

    @NotNull(message = "담당자 역할은 필수입니다")
    private ContactRole role;

    @NotNull(message = "엔티티 유형은 필수입니다")
    private ContactEntityType entityType;

    @NotNull(message = "엔티티 ID는 필수입니다")
    private UUID entityId;

    @Builder.Default
    private Boolean isPrimary = false;
}
