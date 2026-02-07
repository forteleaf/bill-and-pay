package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.enums.EventType;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.exception.settlement.SettlementCalculationException;
import com.korpay.billpay.exception.settlement.ZeroSumViolationException;
import com.korpay.billpay.repository.SettlementRepository;
import com.korpay.billpay.repository.TransactionEventRepository;
import com.korpay.billpay.service.settlement.calculator.PartialCancelCalculator;
import com.korpay.billpay.service.settlement.validator.ZeroSumValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementCreationService {

    private final FeeCalculationService feeCalculationService;
    private final PartialCancelCalculator partialCancelCalculator;
    private final ZeroSumValidator zeroSumValidator;
    private final SettlementRepository settlementRepository;
    private final TransactionEventRepository transactionEventRepository;

    @Transactional
    public List<Settlement> createSettlements(
            TransactionEvent event,
            Merchant merchant,
            String paymentMethodCode) {

        log.info("Creating settlements for event {}: type={}, amount={}",
                event.getId(), event.getEventType(), event.getAmount());

        List<Settlement> settlements;

        if (event.getEventType() == EventType.APPROVAL) {
            settlements = createApprovalSettlements(event, merchant, paymentMethodCode);
        } else if (event.getEventType() == EventType.CANCEL) {
            settlements = createCancelSettlements(event, merchant, paymentMethodCode);
        } else if (event.getEventType() == EventType.PARTIAL_CANCEL) {
            settlements = createPartialCancelSettlements(event);
        } else {
            throw new SettlementCalculationException("Unsupported event type: " + event.getEventType());
        }

        try {
            zeroSumValidator.validate(event, settlements);
        } catch (ZeroSumViolationException e) {
            log.error("Zero-Sum 검증 실패: eventId={}, amount={}, diff={}",
                event.getId(), event.getAmount(), e.getDifference());

            // 정산 전체를 PENDING_REVIEW 상태로 저장 (데이터 유실 방지)
            settlements.forEach(s -> s.setStatus(SettlementStatus.PENDING_REVIEW));
            List<Settlement> savedSettlements = settlementRepository.saveAll(settlements);

            log.warn("Zero-Sum 검증 실패 정산 {} 건을 PENDING_REVIEW로 저장: eventId={}",
                savedSettlements.size(), event.getId());

            return savedSettlements;
        }

        List<Settlement> savedSettlements = settlementRepository.saveAll(settlements);

        log.info("Created {} settlements for event {}", savedSettlements.size(), event.getId());

        return savedSettlements;
    }

    private List<Settlement> createApprovalSettlements(
            TransactionEvent event,
            Merchant merchant,
            String paymentMethodCode) {

        return feeCalculationService.calculateFees(event, merchant, paymentMethodCode);
    }

    private List<Settlement> createCancelSettlements(
            TransactionEvent event,
            Merchant merchant,
            String paymentMethodCode) {

        return feeCalculationService.calculateFees(event, merchant, paymentMethodCode);
    }

    private List<Settlement> createPartialCancelSettlements(TransactionEvent cancelEvent) {
        TransactionEvent approvalEvent = findOriginalApprovalEvent(cancelEvent);
        return partialCancelCalculator.calculateProportional(cancelEvent, approvalEvent);
    }

    private TransactionEvent findOriginalApprovalEvent(TransactionEvent cancelEvent) {
        List<TransactionEvent> events = transactionEventRepository
                .findByTransactionIdOrderByEventSequenceAsc(cancelEvent.getTransactionId());

        return events.stream()
                .filter(e -> e.getEventType() == EventType.APPROVAL)
                .findFirst()
                .orElseThrow(() -> new SettlementCalculationException(
                        "Original approval event not found for transaction: " + cancelEvent.getTransactionId()
                ));
    }
}
