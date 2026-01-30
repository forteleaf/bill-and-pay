package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.SettlementBatch;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementBatchStatus;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.dto.response.SettlementBatchDto;
import com.korpay.billpay.dto.response.SettlementSummaryDto;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.repository.SettlementBatchRepository;
import com.korpay.billpay.repository.SettlementRepository;
import com.korpay.billpay.service.auth.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
    private final SettlementBatchRepository settlementBatchRepository;
    private final AccessControlService accessControlService;

    public Page<Settlement> findAccessibleSettlements(
            User user,
            OrganizationType entityType,
            SettlementStatus status,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Pageable pageable) {
        
        String userPath = accessControlService.isMasterAdmin(user) ? "" : user.getOrgPath();
        String entityTypeStr = entityType != null ? entityType.name() : null;
        String statusStr = status != null ? status.name() : null;
        
        return settlementRepository.findAccessibleSettlements(
                userPath,
                entityTypeStr,
                statusStr,
                startDate,
                endDate,
                pageable
        );
    }

    public SettlementSummaryDto getSummary(
            User user,
            OrganizationType entityType,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        String userPath = accessControlService.isMasterAdmin(user) ? "" : user.getOrgPath();
        String entityTypeStr = entityType != null ? entityType.name() : null;
        
        List<Settlement> filteredSettlements = settlementRepository.findAccessibleSettlementsForSummary(
                userPath,
                entityTypeStr,
                startDate,
                endDate
        );
        
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
        
        String userPath = accessControlService.isMasterAdmin(user) ? "" : user.getOrgPath();
        
        return settlementRepository.findAccessibleSettlementsInDateRange(
                userPath,
                startOfDay,
                endOfDay
        );
    }

    public PagedResponse<SettlementBatchDto> findBatches(
            LocalDate startDate,
            LocalDate endDate,
            SettlementBatchStatus status,
            int page,
            int size) {
        
        // Apply size limit
        if (size > 100) {
            size = 100;
        }
        
        // Query all batches
        List<SettlementBatch> allBatches = settlementBatchRepository.findAll();
        
        // Apply filters
        List<SettlementBatch> filteredBatches = allBatches.stream()
                .filter(batch -> {
                    // Filter by date range
                    if (startDate != null && batch.getSettlementDate().isBefore(startDate)) {
                        return false;
                    }
                    if (endDate != null && batch.getSettlementDate().isAfter(endDate)) {
                        return false;
                    }
                    // Filter by status
                    if (status != null && batch.getStatus() != status) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(SettlementBatch::getSettlementDate).reversed())
                .collect(Collectors.toList());
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, filteredBatches.size());
        
        List<SettlementBatch> pageContent = start < filteredBatches.size() 
                ? filteredBatches.subList(start, end) 
                : Collections.emptyList();
        
        // Convert to DTOs
        List<SettlementBatchDto> dtos = pageContent.stream()
                .map(SettlementBatchDto::from)
                .collect(Collectors.toList());
        
        // Build paged response
        int totalPages = (int) Math.ceil((double) filteredBatches.size() / size);
        
        return PagedResponse.<SettlementBatchDto>builder()
                .content(dtos)
                .page(page)
                .size(size)
                .totalElements(filteredBatches.size())
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }
}
