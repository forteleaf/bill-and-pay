package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.dto.response.SettlementSummaryDto;
import com.korpay.billpay.repository.SettlementRepository;
import com.korpay.billpay.service.auth.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementQueryService {

    private final SettlementRepository settlementRepository;
    private final AccessControlService accessControlService;

    public Page<Settlement> findAccessibleSettlements(
            User user,
            OrganizationType entityType,
            SettlementStatus status,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Pageable pageable) {
        
        List<Settlement> allSettlements = settlementRepository.findAll();
        
        List<Settlement> accessibleSettlements = allSettlements.stream()
                .filter(settlement -> accessControlService.hasAccessToOrganization(user, settlement.getEntityPath()))
                .collect(Collectors.toList());
        
        if (entityType != null) {
            accessibleSettlements = accessibleSettlements.stream()
                    .filter(s -> s.getEntityType() == entityType)
                    .collect(Collectors.toList());
        }
        
        if (status != null) {
            accessibleSettlements = accessibleSettlements.stream()
                    .filter(s -> s.getStatus() == status)
                    .collect(Collectors.toList());
        }
        
        if (startDate != null) {
            accessibleSettlements = accessibleSettlements.stream()
                    .filter(s -> s.getCreatedAt().isAfter(startDate) || s.getCreatedAt().isEqual(startDate))
                    .collect(Collectors.toList());
        }
        
        if (endDate != null) {
            accessibleSettlements = accessibleSettlements.stream()
                    .filter(s -> s.getCreatedAt().isBefore(endDate) || s.getCreatedAt().isEqual(endDate))
                    .collect(Collectors.toList());
        }
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), accessibleSettlements.size());
        
        List<Settlement> pageContent = accessibleSettlements.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, accessibleSettlements.size());
    }

    public SettlementSummaryDto getSummary(
            User user,
            OrganizationType entityType,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        List<Settlement> allSettlements = settlementRepository.findAll();
        
        List<Settlement> filteredSettlements = allSettlements.stream()
                .filter(settlement -> accessControlService.hasAccessToOrganization(user, settlement.getEntityPath()))
                .filter(s -> entityType == null || s.getEntityType() == entityType)
                .filter(s -> startDate == null || s.getCreatedAt().isAfter(startDate) || s.getCreatedAt().isEqual(startDate))
                .filter(s -> endDate == null || s.getCreatedAt().isBefore(endDate) || s.getCreatedAt().isEqual(endDate))
                .collect(Collectors.toList());
        
        long totalAmount = filteredSettlements.stream()
                .mapToLong(Settlement::getAmount)
                .sum();
        
        long totalFeeAmount = filteredSettlements.stream()
                .mapToLong(Settlement::getFeeAmount)
                .sum();
        
        long totalNetAmount = filteredSettlements.stream()
                .mapToLong(Settlement::getNetAmount)
                .sum();
        
        long creditAmount = filteredSettlements.stream()
                .filter(s -> s.getEntryType() == EntryType.CREDIT)
                .mapToLong(Settlement::getAmount)
                .sum();
        
        long debitAmount = filteredSettlements.stream()
                .filter(s -> s.getEntryType() == EntryType.DEBIT)
                .mapToLong(Settlement::getAmount)
                .sum();
        
        return SettlementSummaryDto.builder()
                .entityId(user.getOrganization().getId())
                .entityType(user.getOrganization().getOrgType())
                .entityPath(user.getOrgPath())
                .totalAmount(totalAmount)
                .totalFeeAmount(totalFeeAmount)
                .totalNetAmount(totalNetAmount)
                .creditAmount(creditAmount)
                .debitAmount(debitAmount)
                .transactionCount((long) filteredSettlements.size())
                .currency("KRW")
                .build();
    }

    public List<Settlement> getDailyBatchReport(User user, LocalDate date) {
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        
        List<Settlement> allSettlements = settlementRepository.findAll();
        
        return allSettlements.stream()
                .filter(settlement -> accessControlService.hasAccessToOrganization(user, settlement.getEntityPath()))
                .filter(s -> s.getCreatedAt().isAfter(startOfDay) && s.getCreatedAt().isBefore(endOfDay))
                .collect(Collectors.toList());
    }
}
