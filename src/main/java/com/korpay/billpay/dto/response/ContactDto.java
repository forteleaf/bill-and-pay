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
public class ContactDto {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private ContactRole role;
    private ContactEntityType entityType;
    private UUID entityId;
    private Boolean isPrimary;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static ContactDto from(Contact contact) {
        if (contact == null) {
            return null;
        }
        return ContactDto.builder()
                .id(contact.getId())
                .name(contact.getName())
                .phone(contact.getPhone())
                .email(contact.getEmail())
                .role(contact.getRole())
                .entityType(contact.getEntityType())
                .entityId(contact.getEntityId())
                .isPrimary(contact.getIsPrimary())
                .createdAt(contact.getCreatedAt())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }
}
