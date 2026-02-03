package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.MerchantPgMappingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class MerchantPgMappingCreateRequest {

    @NotNull(message = "가맹점 ID는 필수입니다")
    private UUID merchantId;

    @NotNull(message = "PG 연결 ID는 필수입니다")
    private Long pgConnectionId;

    @NotBlank(message = "MID는 필수입니다")
    @Size(max = 50, message = "MID는 50자를 초과할 수 없습니다")
    private String mid;

    @Size(max = 50, message = "단말기 ID는 50자를 초과할 수 없습니다")
    private String terminalId;

    @Size(max = 50, message = "CAT ID는 50자를 초과할 수 없습니다")
    private String catId;

    private Map<String, Object> config;

    @Builder.Default
    private MerchantPgMappingStatus status = MerchantPgMappingStatus.ACTIVE;
}
