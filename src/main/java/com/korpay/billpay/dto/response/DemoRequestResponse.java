package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.DemoRequest;
import com.korpay.billpay.domain.enums.DemoRequestStatus;
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
public class DemoRequestResponse {

    private UUID id;
    private String companyName;
    private String contactName;
    private String email;
    private String phone;
    private String position;
    private String employeeCount;
    private String monthlyVolume;
    private String message;
    private DemoRequestStatus status;
    private OffsetDateTime createdAt;

    public static DemoRequestResponse from(DemoRequest entity) {
        return DemoRequestResponse.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .contactName(entity.getContactName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .position(entity.getPosition())
                .employeeCount(entity.getEmployeeCount())
                .monthlyVolume(entity.getMonthlyVolume())
                .message(entity.getMessage())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
