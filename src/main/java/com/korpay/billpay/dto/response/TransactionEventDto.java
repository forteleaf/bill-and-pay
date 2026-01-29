package com.korpay.billpay.dto.response;

import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.enums.EventType;
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
public class TransactionEventDto {
    
    private UUID id;
    private UUID transactionId;
    private EventType eventType;
    private Long amount;
    private String currency;
    private UUID relatedEventId;
    private Map<String, Object> metadata;
    private OffsetDateTime eventAt;
    private OffsetDateTime createdAt;
    
    public static TransactionEventDto from(TransactionEvent event) {
        return TransactionEventDto.builder()
                .id(event.getId())
                .transactionId(event.getTransactionId())
                .eventType(event.getEventType())
                .amount(event.getAmount())
                .currency(event.getCurrency())
                .relatedEventId(event.getRelatedEventId())
                .metadata(event.getMetadata())
                .eventAt(event.getEventAt())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
