package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.TerminalStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminalUpdateRequest {

    @Size(max = 50, message = "CAT ID는 50자 이하여야 합니다")
    private String catId;

    @Size(max = 100, message = "시리얼번호는 100자 이하여야 합니다")
    private String serialNumber;

    @Size(max = 100, message = "모델명은 100자 이하여야 합니다")
    private String model;

    @Size(max = 100, message = "제조사는 100자 이하여야 합니다")
    private String manufacturer;

    private String installAddress;

    private LocalDate installDate;

    private TerminalStatus status;

    private Map<String, Object> config;
}
