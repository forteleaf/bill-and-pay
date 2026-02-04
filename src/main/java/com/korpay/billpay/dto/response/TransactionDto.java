package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.Transaction;
import com.korpay.billpay.domain.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    
    private UUID id;
    private String transactionId;
    private UUID merchantId;
    private String merchantName;
    private String merchantPath;
    private String orgPath;
    private Long pgConnectionId;
    private UUID paymentMethodId;
    private UUID cardCompanyId;
    private Long amount;
    private String currency;
    private TransactionStatus status;
    private String pgTransactionId;
    private String approvalNumber;
    private OffsetDateTime approvedAt;
    private OffsetDateTime cancelledAt;
    private String catId;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    
    public static TransactionDto from(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .transactionId(transaction.getTransactionId())
                .merchantId(transaction.getMerchant().getId())
                .merchantName(transaction.getMerchant().getName())
                .merchantPath(transaction.getMerchantPath())
                .orgPath(transaction.getOrgPath())
                .pgConnectionId(transaction.getPgConnectionId())
                .paymentMethodId(transaction.getPaymentMethod().getId())
                .cardCompanyId(transaction.getCardCompany() != null ? transaction.getCardCompany().getId() : null)
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus())
                .pgTransactionId(transaction.getPgTransactionId())
                .approvalNumber(transaction.getApprovalNumber())
                .approvedAt(transaction.getApprovedAt())
                .cancelledAt(transaction.getCancelledAt())
                .catId(transaction.getCatId())
                .metadata(transaction.getMetadata())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
