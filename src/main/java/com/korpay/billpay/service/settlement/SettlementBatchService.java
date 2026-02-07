package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.SettlementBatch;
import com.korpay.billpay.domain.enums.SettlementBatchStatus;
import com.korpay.billpay.domain.enums.SettlementCycle;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.repository.SettlementBatchRepository;
import com.korpay.billpay.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SettlementBatchRepository settlementBatchRepository;
    private final SettlementRepository settlementRepository;
    private final BusinessDayCalculator businessDayCalculator;

    @Transactional
    public SettlementBatch createDailyBatch(LocalDate transactionDate, SettlementCycle cycle) {
        LocalDate settlementDate = businessDayCalculator.calculateSettlementDate(transactionDate, cycle);

        String cyclePrefix = cycleToBatchPrefix(cycle);
        if (settlementBatchRepository.existsBySettlementDateAndBatchNumberContaining(settlementDate, cyclePrefix)) {
            log.info("Batch already exists for date={}, cycle={}", settlementDate, cycle);
            return null;
        }

        OffsetDateTime periodStart = transactionDate.atStartOfDay(KST).toOffsetDateTime();
        OffsetDateTime periodEnd = transactionDate.atTime(LocalTime.MAX).atZone(KST).toOffsetDateTime();

        List<Settlement> unbatched = settlementRepository.findUnbatchedSettlementsByCycle(
                periodStart, periodEnd, cycle.name());

        if (unbatched.isEmpty()) {
            log.info("No unbatched settlements for date={}, cycle={}", transactionDate, cycle);
            return null;
        }

        String batchNumber = generateBatchNumber(settlementDate, cycle);

        long totalAmount = unbatched.stream().mapToLong(Settlement::getAmount).sum();
        long totalFeeAmount = unbatched.stream().mapToLong(Settlement::getFeeAmount).sum();

        SettlementBatch batch = SettlementBatch.builder()
                .batchNumber(batchNumber)
                .settlementDate(settlementDate)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status(SettlementBatchStatus.PROCESSING)
                .totalTransactions(unbatched.size())
                .totalAmount(totalAmount)
                .totalFeeAmount(totalFeeAmount)
                .metadata(Map.of(
                        "cycle", cycle.name(),
                        "transactionDate", transactionDate.toString()))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        batch = settlementBatchRepository.save(batch);

        for (Settlement settlement : unbatched) {
            settlement.setSettlementBatch(batch);
            settlement.setStatus(SettlementStatus.COMPLETED);
            settlement.setSettledAt(OffsetDateTime.now());
            settlement.setUpdatedAt(OffsetDateTime.now());
        }
        settlementRepository.saveAll(unbatched);

        batch.setStatus(SettlementBatchStatus.COMPLETED);
        batch.setProcessedAt(OffsetDateTime.now());
        batch.setUpdatedAt(OffsetDateTime.now());
        settlementBatchRepository.save(batch);

        log.info("Created batch {}: {} settlements, amount={}, fee={}",
                batchNumber, unbatched.size(), totalAmount, totalFeeAmount);

        return batch;
    }

    @Transactional
    public SettlementBatch createRealtimeBatch(LocalDate today) {
        return createDailyBatch(today, SettlementCycle.REALTIME);
    }

    @Transactional
    public int backfillUnbatchedSettlements() {
        List<Settlement> unbatched = settlementRepository.findAll().stream()
                .filter(s -> s.getSettlementBatch() == null && s.getStatus() == SettlementStatus.PENDING)
                .toList();

        if (unbatched.isEmpty()) {
            log.info("No unbatched settlements found for backfill");
            return 0;
        }

        Map<LocalDate, List<Settlement>> byDate = new java.util.LinkedHashMap<>();
        for (Settlement s : unbatched) {
            LocalDate date = s.getCreatedAt().atZoneSameInstant(KST).toLocalDate();
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(s);
        }

        int totalBatches = 0;
        SettlementCycle[] cycles = { SettlementCycle.D_PLUS_1, SettlementCycle.D_PLUS_3 };

        for (LocalDate transactionDate : byDate.keySet()) {
            for (SettlementCycle cycle : cycles) {
                try {
                    SettlementBatch batch = createDailyBatch(transactionDate, cycle);
                    if (batch != null) {
                        totalBatches++;
                        log.info("Backfill: created batch for date={}, cycle={}", transactionDate, cycle);
                    }
                } catch (Exception e) {
                    log.warn("Backfill: failed for date={}, cycle={}: {}", transactionDate, cycle, e.getMessage());
                }
            }
        }

        log.info("Backfill completed: {} batches created from {} dates", totalBatches, byDate.size());
        return totalBatches;
    }

    private String generateBatchNumber(LocalDate date, SettlementCycle cycle) {
        String prefix = cycleToBatchPrefix(cycle);
        String dateStr = date.format(DATE_FORMAT);

        List<SettlementBatch> existing = settlementBatchRepository.findBySettlementDate(date);
        long count = existing.stream()
                .filter(b -> b.getBatchNumber().contains(prefix))
                .count();

        return String.format("BATCH-%s-%s-%03d", prefix, dateStr, count + 1);
    }

    private String cycleToBatchPrefix(SettlementCycle cycle) {
        return switch (cycle) {
            case D_PLUS_1 -> "D1";
            case D_PLUS_3 -> "D3";
            case REALTIME -> "RT";
        };
    }
}
