package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.SettlementAccount;
import com.korpay.billpay.domain.enums.AccountStatus;
import com.korpay.billpay.domain.enums.ContactEntityType;
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
public class SettlementAccountResponse {

    private UUID id;
    private String bankCode;
    private String bankName;
    private String accountNumber;
    private String maskedAccountNumber;
    private String accountHolder;
    private ContactEntityType entityType;
    private UUID entityId;
    private Boolean isPrimary;
    private AccountStatus status;
    private String statusDescription;
    private OffsetDateTime verifiedAt;
    private String memo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static SettlementAccountResponse from(SettlementAccount account) {
        return SettlementAccountResponse.builder()
                .id(account.getId())
                .bankCode(account.getBankCode())
                .bankName(account.getBankName())
                .accountNumber(account.getAccountNumber())
                .maskedAccountNumber(maskAccountNumber(account.getAccountNumber()))
                .accountHolder(account.getAccountHolder())
                .entityType(account.getEntityType())
                .entityId(account.getEntityId())
                .isPrimary(account.getIsPrimary())
                .status(account.getStatus())
                .statusDescription(getStatusDescription(account.getStatus()))
                .verifiedAt(account.getVerifiedAt())
                .memo(account.getMemo())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private static String getStatusDescription(AccountStatus status) {
        return switch (status) {
            case ACTIVE -> "사용중";
            case INACTIVE -> "미사용";
            case PENDING_VERIFICATION -> "검증대기";
        };
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 8) {
            return accountNumber;
        }
        String first = accountNumber.substring(0, 4);
        String last = accountNumber.substring(accountNumber.length() - 4);
        String masked = "*".repeat(accountNumber.length() - 8);
        return first + masked + last;
    }
}
