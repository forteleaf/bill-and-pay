package com.korpay.billpay.dto.request;

import com.korpay.billpay.domain.enums.MerchantPgMappingStatus;
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
public class MerchantPgMappingUpdateRequest {

    private UUID pgConnectionId;

    @Size(max = 50, message = "MID는 50자를 초과할 수 없습니다")
    private String mid;

    @Size(max = 50, message = "단말기 ID는 50자를 초과할 수 없습니다")
    private String terminalId;

    @Size(max = 50, message = "CAT ID는 50자를 초과할 수 없습니다")
    private String catId;

    private Map<String, Object> config;

    private MerchantPgMappingStatus status;
}
