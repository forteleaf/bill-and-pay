package com.korpay.billpay.dto.webhook;

import com.korpay.billpay.domain.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private String pgTid;
    private String pgOtid;
    private String pgMerchantNo;
    private String terminalId;
    private String channelType;
    private String vanTid;

    private String orderId;
    private Long amount;
    private Long remainAmount;
    private String paymentMethod;
    private String goodsName;
    private String currency;

    private String cardNoMasked;
    private String approvalNo;
    private Integer installment;
    private String issuerCode;
    private String acquirerCode;
    private String cardCompanyName;

    private String buyerName;
    private String buyerId;

    private EventType eventType;
    private Boolean isCancel;
    private OffsetDateTime transactedAt;
    private OffsetDateTime canceledAt;

    private Map<String, Object> metadata;
}
