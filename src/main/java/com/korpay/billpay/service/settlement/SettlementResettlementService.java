package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.TransactionEvent;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.repository.SettlementRepository;
import com.korpay.billpay.repository.TransactionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementResettlementService {

    private static final List<SettlementStatus> RESETTLEABLE_STATUSES = List.of(
            SettlementStatus.FAILED,
            SettlementStatus.PENDING_REVIEW
    );

    private final SettlementRepository settlementRepository;
    private final TransactionEventRepository transactionEventRepository;
    private final SettlementService settlementService;

    @Transactional
    public List<Settlement> resettleByTransactionEventId(UUID transactionEventId) {
        log.info("재정산 시작: transactionEventId={}", transactionEventId);

        // 1. 기존 정산 조회 (FAILED, PENDING_REVIEW만)
        List<Settlement> existingSettlements = settlementRepository
                .findByTransactionEventIdAndStatusIn(transactionEventId, RESETTLEABLE_STATUSES);

        if (existingSettlements.isEmpty()) {
            throw new IllegalStateException(
                    "재정산 가능한 정산이 없습니다. FAILED 또는 PENDING_REVIEW 상태의 정산만 재정산할 수 있습니다.");
        }

        // 2. 기존 정산 전체를 CANCELLED 상태로 변경
        existingSettlements.forEach(s -> s.setStatus(SettlementStatus.CANCELLED));
        settlementRepository.saveAll(existingSettlements);
        log.info("기존 정산 {} 건을 CANCELLED 처리: transactionEventId={}", existingSettlements.size(), transactionEventId);

        // 3. TransactionEvent 조회
        TransactionEvent event = transactionEventRepository.findByIdOnly(transactionEventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "거래 이벤트를 찾을 수 없습니다: " + transactionEventId));

        // 4. SettlementService.processTransactionEvent()로 재생성
        List<Settlement> newSettlements = settlementService.processTransactionEvent(event);

        log.info("재정산 완료: transactionEventId={}, 기존 {} 건 취소, 신규 {} 건 생성",
                transactionEventId, existingSettlements.size(), newSettlements.size());

        return newSettlements;
    }
}
