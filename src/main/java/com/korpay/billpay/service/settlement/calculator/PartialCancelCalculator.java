package com.korpay.billpay.service.settlement.calculator;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.exception.settlement.OriginalSettlementNotFoundException;
import com.korpay.billpay.exception.settlement.ZeroSumViolationException;
import com.korpay.billpay.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartialCancelCalculator {

    private final SettlementRepository settlementRepository;

    public List<Settlement> calculateProportional(
            TransactionEvent cancelEvent,
            TransactionEvent originalApprovalEvent) {

        List<Settlement> originalSettlements = settlementRepository
                .findByTransactionEventId(originalApprovalEvent.getId());

        if (originalSettlements.isEmpty()) {
            throw new OriginalSettlementNotFoundException(
                    cancelEvent.getTransactionId(),
                    cancelEvent.getId()
            );
        }

        BigDecimal cancelRatio = calculateCancelRatio(cancelEvent, originalApprovalEvent);
        log.info("Calculating partial cancel with ratio {} for event {}",
                cancelRatio, cancelEvent.getId());

        List<Settlement> cancelSettlements = new ArrayList<>();
        for (Settlement original : originalSettlements) {
            long cancelAmount = calculateProportionalAmount(original.getAmount(), cancelRatio);

            Settlement cancelSettlement = Settlement.builder()
                    .transactionEventId(cancelEvent.getId())
                    .transactionId(cancelEvent.getTransactionId())
                    .merchantId(cancelEvent.getMerchantId())
                    .merchantPath(cancelEvent.getMerchantPath())
                    .entityId(original.getEntityId())
                    .entityType(original.getEntityType())
                    .entityPath(original.getEntityPath())
                    .entryType(EntryType.DEBIT)
                    .amount(cancelAmount)
                    .feeAmount(0L)
                    .netAmount(cancelAmount)
                    .currency(original.getCurrency())
                    .feeRate(original.getFeeRate())
                    .feeConfig(original.getFeeConfig())
                    .status(SettlementStatus.PENDING)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();

            cancelSettlements.add(cancelSettlement);
        }

        adjustRoundingDifferenceToMaster(cancelEvent, cancelSettlements);

        return cancelSettlements;
    }

    private BigDecimal calculateCancelRatio(TransactionEvent cancelEvent, TransactionEvent approvalEvent) {
        long cancelAbsAmount = Math.abs(cancelEvent.getAmount());
        long approvalAmount = approvalEvent.getAmount();

        return BigDecimal.valueOf(cancelAbsAmount)
                .divide(BigDecimal.valueOf(approvalAmount), 10, RoundingMode.HALF_UP);
    }

    private long calculateProportionalAmount(long originalAmount, BigDecimal ratio) {
        return BigDecimal.valueOf(originalAmount)
                .multiply(ratio)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();
    }

    private void adjustRoundingDifferenceToMaster(
            TransactionEvent cancelEvent,
            List<Settlement> cancelSettlements) {

        long targetAmount = Math.abs(cancelEvent.getAmount());
        long currentTotal = cancelSettlements.stream()
                .mapToLong(Settlement::getAmount)
                .sum();

        long difference = targetAmount - currentTotal;

        if (difference == 0) {
            log.debug("No rounding difference for event {}", cancelEvent.getId());
            return;
        }

        Settlement masterSettlement = findMasterSettlement(cancelSettlements);
        long newMasterAmount = masterSettlement.getAmount() + difference;
        masterSettlement.setAmount(newMasterAmount);
        masterSettlement.setNetAmount(newMasterAmount);

        log.info("Adjusted {} rounding difference to MASTER: {} -> {}",
                difference, masterSettlement.getAmount() - difference, newMasterAmount);

        validateFinalZeroSum(targetAmount, cancelSettlements);
    }

    private Settlement findMasterSettlement(List<Settlement> settlements) {
        return settlements.stream()
                .filter(s -> s.getEntityType() == OrganizationType.DISTRIBUTOR)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MASTER settlement not found in cancel settlements"));
    }

    private void validateFinalZeroSum(long targetAmount, List<Settlement> settlements) {
        long finalTotal = settlements.stream()
                .mapToLong(Settlement::getAmount)
                .sum();

        if (finalTotal != targetAmount) {
            throw new ZeroSumViolationException(
                    settlements.get(0).getTransactionEventId(),
                    -targetAmount,
                    finalTotal
            );
        }
    }
}
