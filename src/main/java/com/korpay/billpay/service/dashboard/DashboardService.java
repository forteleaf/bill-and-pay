package com.korpay.billpay.service.dashboard;

import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.domain.enums.TransactionStatus;
import com.korpay.billpay.dto.response.DashboardMetricsDto;
import com.korpay.billpay.dto.response.MerchantRankingDto;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.SettlementRepository;
import com.korpay.billpay.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;
    private final MerchantRepository merchantRepository;

    public DashboardMetricsDto getMetrics(User currentUser) {
        String orgPath = currentUser.getOrganization().getPath();
        
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        OffsetDateTime startOfToday = now.truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        
        Long todaySales = transactionRepository
                .sumAmountByOrgPathAndStatusAndDateRange(
                        orgPath, 
                        TransactionStatus.APPROVED, 
                        startOfToday, 
                        now);
        
        Long monthSales = transactionRepository
                .sumAmountByOrgPathAndStatusAndDateRange(
                        orgPath, 
                        TransactionStatus.APPROVED, 
                        startOfMonth, 
                        now);
        
        Long pendingSettlements = settlementRepository
                .countByEntityPathStartingWithAndStatus(
                        orgPath, 
                        SettlementStatus.PENDING);
        
        Long transactionCount = transactionRepository
                .countByOrgPathStartingWithAndCreatedAtBetween(
                        orgPath, 
                        startOfMonth, 
                        now);
        
        return DashboardMetricsDto.builder()
                .todaySales(todaySales != null ? todaySales : 0L)
                .monthSales(monthSales != null ? monthSales : 0L)
                .pendingSettlements(pendingSettlements != null ? pendingSettlements : 0L)
                .transactionCount(transactionCount != null ? transactionCount : 0L)
                .build();
    }

    public List<MerchantRankingDto> getTopMerchants(User currentUser, int limit) {
        String orgPath = currentUser.getOrganization().getPath();
        
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        OffsetDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        
        List<Object[]> results = transactionRepository
                .findTopMerchantsByAmountAndOrgPath(orgPath, startOfMonth, now, limit);
        
        return results.stream()
                .map(row -> MerchantRankingDto.builder()
                        .merchantId(row[0] != null ? (java.util.UUID) row[0] : null)
                        .merchantName(row[1] != null ? (String) row[1] : "Unknown")
                        .totalAmount(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                        .transactionCount(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());
    }
}
