package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.SettlementBatch;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.EntryType;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementBatchStatus;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.dto.response.OrganizationSettlementDetailDto;
import com.korpay.billpay.dto.response.OrganizationSettlementDetailDto.HierarchyFeeDto;
import com.korpay.billpay.dto.response.OrganizationSettlementDetailDto.MerchantSettlementDto;
import com.korpay.billpay.dto.response.OrganizationSettlementDetailDto.SettlementCalculationDto;
import com.korpay.billpay.dto.response.OrganizationSettlementSummaryDto;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.dto.response.SettlementBatchDto;
import com.korpay.billpay.dto.response.SettlementSummaryDto;
import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.SettlementBatchRepository;
import com.korpay.billpay.repository.SettlementRepository;
import com.korpay.billpay.repository.SettlementRepository.HierarchyFeeAggregation;
import com.korpay.billpay.repository.SettlementRepository.MerchantSettlementAggregation;
import com.korpay.billpay.repository.SettlementRepository.OrganizationSettlementAggregation;
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
    private final MerchantRepository merchantRepository;
    private final AccessControlService accessControlService;

    public Page<Settlement> findAccessibleSettlements(
            User user,
            OrganizationType entityType,
            SettlementStatus status,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            boolean merchantOnly,
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
                merchantOnly,
                pageable
        );
    }

    public Map<UUID, String> getMerchantNamesByIds(List<UUID> merchantIds) {
        if (merchantIds == null || merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Merchant> merchants = merchantRepository.findAllById(merchantIds);
        return merchants.stream()
                .collect(Collectors.toMap(Merchant::getId, Merchant::getName, (a, b) -> a));
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

    public List<OrganizationSettlementSummaryDto> getSettlementsByOrganization(
            User user,
            OrganizationType orgType,
            String search,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        String userPath = accessControlService.isMasterAdmin(user) ? "" : user.getOrgPath();
        String orgTypeStr = orgType != null ? orgType.name() : null;
        
        List<OrganizationSettlementAggregation> aggregations = 
                settlementRepository.aggregateSettlementsByOrganization(
                        userPath, orgTypeStr, search, startDate, endDate);
        
        return aggregations.stream()
                .map(this::mapToOrganizationSettlementSummary)
                .collect(Collectors.toList());
    }

    private OrganizationSettlementSummaryDto mapToOrganizationSettlementSummary(
            OrganizationSettlementAggregation agg) {
        
        SettlementStatus primaryStatus = determinePrimaryStatus(
                agg.getCompletedCount(), agg.getPendingCount(), agg.getFailedCount());
        
        return OrganizationSettlementSummaryDto.builder()
                .organizationId(agg.getOrganizationId())
                .orgCode(agg.getOrgCode())
                .orgName(agg.getOrgName())
                .orgType(agg.getOrgType() != null ? OrganizationType.valueOf(agg.getOrgType()) : null)
                .orgPath(agg.getOrgPath())
                .level(agg.getLevel())
                .businessEntityId(agg.getBusinessEntityId())
                .representativeName(agg.getRepresentativeName())
                .mainPhone(agg.getMainPhone())
                .merchantCount(agg.getMerchantCount())
                .approvalAmount(agg.getApprovalAmount())
                .approvalCount(agg.getApprovalCount())
                .cancelAmount(agg.getCancelAmount())
                .cancelCount(agg.getCancelCount())
                .netPaymentAmount(agg.getNetPaymentAmount())
                .totalTransactionCount(agg.getTotalTransactionCount())
                .merchantFeeAmount(agg.getMerchantFeeAmount())
                .orgFeeAmount(agg.getOrgFeeAmount())
                .avgFeeRate(agg.getAvgFeeRate())
                .primaryStatus(primaryStatus)
                .completedCount(agg.getCompletedCount())
                .pendingCount(agg.getPendingCount())
                .failedCount(agg.getFailedCount())
                .currency("KRW")
                .build();
    }

    private SettlementStatus determinePrimaryStatus(Long completed, Long pending, Long failed) {
        if (failed != null && failed > 0) {
            return SettlementStatus.FAILED;
        }
        if (pending != null && pending > 0) {
            return SettlementStatus.PENDING;
        }
        if (completed != null && completed > 0) {
            return SettlementStatus.COMPLETED;
        }
        return null;
    }

    public OrganizationSettlementDetailDto getOrganizationSettlementDetail(
            User user,
            UUID organizationId,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        String userPath = accessControlService.isMasterAdmin(user) ? "" : user.getOrgPath();
        String orgTypeStr = null;
        
        List<OrganizationSettlementAggregation> orgAggs = 
                settlementRepository.aggregateSettlementsByOrganization(
                        userPath, orgTypeStr, null, startDate, endDate);
        
        OrganizationSettlementAggregation orgAgg = orgAggs.stream()
                .filter(agg -> organizationId.equals(agg.getOrganizationId()))
                .findFirst()
                .orElse(null);
        
        if (orgAgg == null) {
            return null;
        }
        
        String orgPath = orgAgg.getOrgPath();
        
        List<MerchantSettlementAggregation> merchantAggs = 
                settlementRepository.aggregateMerchantSettlements(orgPath, startDate, endDate);
        
        List<HierarchyFeeAggregation> hierarchyAggs = 
                settlementRepository.aggregateHierarchyFees(orgPath, startDate, endDate);
        
        OrganizationSettlementSummaryDto summary = mapToOrganizationSettlementSummary(orgAgg);
        
        List<MerchantSettlementDto> merchantSettlements = merchantAggs.stream()
                .map(this::mapToMerchantSettlement)
                .collect(Collectors.toList());
        
        List<HierarchyFeeDto> hierarchyFees = hierarchyAggs.stream()
                .map(this::mapToHierarchyFee)
                .collect(Collectors.toList());
        
        SettlementCalculationDto calculation = calculateSettlement(summary, hierarchyFees);
        
        return OrganizationSettlementDetailDto.builder()
                .organizationId(orgAgg.getOrganizationId())
                .orgCode(orgAgg.getOrgCode())
                .orgName(orgAgg.getOrgName())
                .orgType(orgAgg.getOrgType() != null ? OrganizationType.valueOf(orgAgg.getOrgType()) : null)
                .orgPath(orgPath)
                .summary(summary)
                .merchantSettlements(merchantSettlements)
                .hierarchyFees(hierarchyFees)
                .calculation(calculation)
                .build();
    }

    private MerchantSettlementDto mapToMerchantSettlement(MerchantSettlementAggregation agg) {
        return MerchantSettlementDto.builder()
                .merchantId(agg.getMerchantId())
                .merchantCode(agg.getMerchantCode())
                .merchantName(agg.getMerchantName())
                .transactionDate(agg.getTransactionDate())
                .branchName(agg.getBranchName())
                .approvalAmount(agg.getApprovalAmount())
                .approvalCount(agg.getApprovalCount())
                .cancelAmount(agg.getCancelAmount())
                .cancelCount(agg.getCancelCount())
                .netPaymentAmount(agg.getNetPaymentAmount())
                .feeAmount(agg.getFeeAmount())
                .feeRate(agg.getFeeRate())
                .build();
    }

    private HierarchyFeeDto mapToHierarchyFee(HierarchyFeeAggregation agg) {
        return HierarchyFeeDto.builder()
                .entityType(agg.getEntityType() != null ? OrganizationType.valueOf(agg.getEntityType()) : null)
                .entityName(agg.getEntityName())
                .entityCode(agg.getEntityCode())
                .feeRate(agg.getFeeRate())
                .feeAmount(agg.getFeeAmount())
                .netAmount(agg.getNetAmount())
                .build();
    }

    private SettlementCalculationDto calculateSettlement(
            OrganizationSettlementSummaryDto summary, 
            List<HierarchyFeeDto> hierarchyFees) {
        
        long grossAmount = summary.getNetPaymentAmount() != null ? summary.getNetPaymentAmount() : 0L;
        long vatAmount = Math.round(grossAmount / 11.0);
        long supplyAmount = grossAmount - vatAmount;
        
        long childOrgFeeAmount = hierarchyFees.stream()
                .filter(h -> h.getEntityType() != null && 
                        h.getEntityType().ordinal() > summary.getOrgType().ordinal())
                .mapToLong(h -> h.getNetAmount() != null ? h.getNetAmount() : 0L)
                .sum();
        
        long merchantPayoutAmount = grossAmount - 
                (summary.getMerchantFeeAmount() != null ? summary.getMerchantFeeAmount() : 0L);
        
        return SettlementCalculationDto.builder()
                .settlementFeeRate(summary.getAvgFeeRate())
                .grossAmount(grossAmount)
                .supplyAmount(supplyAmount)
                .vatAmount(vatAmount)
                .finalAmount(grossAmount)
                .childOrgFeeAmount(childOrgFeeAmount)
                .merchantPayoutAmount(merchantPayoutAmount)
                .build();
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
