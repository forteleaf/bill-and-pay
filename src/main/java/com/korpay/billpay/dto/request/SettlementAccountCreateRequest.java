package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.AccountStatus;
import com.korpay.billpay.domain.enums.ContactEntityType;
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
public class SettlementAccountCreateRequest {

    @NotBlank(message = "은행코드는 필수입니다")
    @Size(max = 10, message = "은행코드는 10자 이하여야 합니다")
    private String bankCode;

    @NotBlank(message = "은행명은 필수입니다")
    @Size(max = 50, message = "은행명은 50자 이하여야 합니다")
    private String bankName;

    @NotBlank(message = "계좌번호는 필수입니다")
    @Size(max = 50, message = "계좌번호는 50자 이하여야 합니다")
    private String accountNumber;

    @NotBlank(message = "예금주는 필수입니다")
    @Size(max = 100, message = "예금주는 100자 이하여야 합니다")
    private String accountHolder;

    @NotNull(message = "엔티티 유형은 필수입니다")
    private ContactEntityType entityType;

    @NotNull(message = "엔티티 ID는 필수입니다")
    private UUID entityId;

    @Builder.Default
    private Boolean isPrimary = false;

    private AccountStatus status;

    @Size(max = 500, message = "메모는 500자 이하여야 합니다")
    private String memo;
}
