package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.TerminalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalCreateRequest {

    @NotBlank(message = "CAT ID는 필수입니다")
    @Size(max = 50, message = "CAT ID는 50자 이하여야 합니다")
    private String catId;

    @NotNull(message = "단말기 유형은 필수입니다")
    private TerminalType terminalType;

    @NotNull(message = "가맹점 ID는 필수입니다")
    private UUID merchantId;

    private UUID organizationId;

    @Size(max = 100, message = "시리얼번호는 100자 이하여야 합니다")
    private String serialNumber;

    @Size(max = 100, message = "모델명은 100자 이하여야 합니다")
    private String model;

    @Size(max = 100, message = "제조사는 100자 이하여야 합니다")
    private String manufacturer;

    private String installAddress;

    private LocalDate installDate;

    private Map<String, Object> config;
}
