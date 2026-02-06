package com.korpay.billpay.service.settlement.validator;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.exception.settlement.ZeroSumViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class ZeroSumValidator {

    public void validate(TransactionEvent event, List<Settlement> settlements) {
        long eventAmount = event.getAmount();
        long settlementTotal = settlements.stream()
                .mapToLong(Settlement::getAmount)
                .sum();

        if (eventAmount != settlementTotal) {
            log.error("Zero-Sum validation failed for event {}: event={}, settlements={}, diff={}",
                    event.getId(), eventAmount, settlementTotal, eventAmount - settlementTotal);

            logSettlementBreakdown(settlements);

            throw new ZeroSumViolationException(event.getId(), eventAmount, settlementTotal);
        }

        log.debug("Zero-Sum validation passed for event {}: {}", event.getId(), eventAmount);
    }

    private void logSettlementBreakdown(List<Settlement> settlements) {
        log.error("Settlement breakdown:");
        settlements.forEach(s ->
                log.error("  {} {} {}: {} (rate={})",
                        s.getEntryType(),
                        s.getEntityType(),
                        s.getEntityId(),
                        s.getAmount(),
                        s.getFeeRate())
        );
    }
}
