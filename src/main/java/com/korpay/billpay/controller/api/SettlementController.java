package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementBatchStatus;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.OrganizationSettlementDetailDto;
import com.korpay.billpay.dto.response.OrganizationSettlementSummaryDto;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.dto.response.SettlementBatchDto;
import com.korpay.billpay.dto.response.SettlementDto;
import com.korpay.billpay.dto.response.SettlementSummaryDto;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.settlement.SettlementQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/settlements")
@RequiredArgsConstructor
@Validated
public class SettlementController {

    private final SettlementQueryService settlementQueryService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SettlementDto>>> listSettlements(
            @RequestParam(required = false) OrganizationType entityType,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Settlement> settlementsPage = settlementQueryService.findAccessibleSettlements(
                currentUser, entityType, status, startDate, endDate, pageable);
        
        List<SettlementDto> dtos = settlementsPage.getContent().stream()
                .map(SettlementDto::from)
                .collect(Collectors.toList());
        
        PagedResponse<SettlementDto> pagedResponse = PagedResponse.of(settlementsPage, dtos);
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SettlementSummaryDto>> getSettlementSummary(
            @RequestParam(required = false) OrganizationType entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        SettlementSummaryDto summary = settlementQueryService.getSummary(
                currentUser, entityType, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/batch/{date}")
    public ResponseEntity<ApiResponse<List<SettlementDto>>> getDailyBatchReport(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        List<Settlement> settlements = settlementQueryService.getDailyBatchReport(currentUser, date);
        List<SettlementDto> dtos = settlements.stream()
                .map(SettlementDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<PagedResponse<SettlementBatchDto>>> listBatches(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) SettlementBatchStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PagedResponse<SettlementBatchDto> result = settlementQueryService.findBatches(
                startDate, endDate, status, page, size);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/by-organization")
    public ResponseEntity<ApiResponse<List<OrganizationSettlementSummaryDto>>> getSettlementsByOrganization(
            @RequestParam(required = false) OrganizationType orgType,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        List<OrganizationSettlementSummaryDto> result = settlementQueryService.getSettlementsByOrganization(
                currentUser, orgType, search, startDate, endDate);
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/organization/{organizationId}/details")
    public ResponseEntity<ApiResponse<OrganizationSettlementDetailDto>> getOrganizationSettlementDetail(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        OrganizationSettlementDetailDto result = settlementQueryService.getOrganizationSettlementDetail(
                currentUser, organizationId, startDate, endDate);
        
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
