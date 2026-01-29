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
        long eventAbsAmount = Math.abs(event.getAmount());
        long settlementTotal = settlements.stream()
                .mapToLong(Settlement::getAmount)
                .sum();

        if (eventAbsAmount != settlementTotal) {
            log.error("Zero-Sum validation failed for event {}: event={}, settlements={}, diff={}",
                    event.getId(), eventAbsAmount, settlementTotal, eventAbsAmount - settlementTotal);

            logSettlementBreakdown(settlements);

            throw new ZeroSumViolationException(event.getId(), event.getAmount(), settlementTotal);
        }

        log.debug("Zero-Sum validation passed for event {}: {}", event.getId(), eventAbsAmount);
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
