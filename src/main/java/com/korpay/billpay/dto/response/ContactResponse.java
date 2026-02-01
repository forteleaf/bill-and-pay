package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.Contact;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.domain.enums.ContactRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactResponse {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private ContactRole role;
    private String roleDescription;
    private ContactEntityType entityType;
    private UUID entityId;
    private Boolean isPrimary;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ContactResponse from(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .name(contact.getName())
                .phone(contact.getPhone())
                .email(contact.getEmail())
                .role(contact.getRole())
                .roleDescription(getRoleDescription(contact.getRole()))
                .entityType(contact.getEntityType())
                .entityId(contact.getEntityId())
                .isPrimary(contact.getIsPrimary())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }

    private static String getRoleDescription(ContactRole role) {
        return switch (role) {
            case PRIMARY -> "주담당자";
            case SECONDARY -> "부담당자";
            case SETTLEMENT -> "정산담당";
            case TECHNICAL -> "기술담당";
        };
    }
}
