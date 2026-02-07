package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.Settlement;
import com.korpay.billpay.domain.entity.SettlementBatch;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.domain.enums.SettlementBatchStatus;
import com.korpay.billpay.domain.enums.SettlementCycle;
import com.korpay.billpay.domain.enums.SettlementStatus;
import com.korpay.billpay.dto.request.ResettleRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.DailySettlementDetailDto;
import com.korpay.billpay.dto.response.DailySettlementSummaryDto;
import com.korpay.billpay.dto.response.MerchantStatementDto;
import com.korpay.billpay.dto.response.OrganizationSettlementDetailDto;
import com.korpay.billpay.dto.response.OrganizationSettlementSummaryDto;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.dto.response.SettlementBatchDto;
import com.korpay.billpay.dto.response.SettlementDto;
import com.korpay.billpay.dto.response.SettlementSummaryDto;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.settlement.DailySettlementService;
import com.korpay.billpay.service.settlement.SettlementBatchService;
import com.korpay.billpay.service.settlement.SettlementQueryService;
import com.korpay.billpay.service.settlement.SettlementResettlementService;
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
    private final SettlementBatchService settlementBatchService;
    private final SettlementResettlementService settlementResettlementService;
    private final DailySettlementService dailySettlementService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SettlementDto>>> listSettlements(
            @RequestParam(required = false) OrganizationType entityType,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @RequestParam(defaultValue = "false") boolean merchantOnly,
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
                currentUser, entityType, status, startDate, endDate, merchantOnly, pageable);
        
        List<UUID> merchantIds = settlementsPage.getContent().stream()
                .map(Settlement::getMerchantId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        java.util.Map<UUID, String> merchantNameMap = settlementQueryService.getMerchantNamesByIds(merchantIds);
        
        List<SettlementDto> dtos = settlementsPage.getContent().stream()
                .map(s -> SettlementDto.from(s, merchantNameMap.get(s.getMerchantId())))
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

    @PostMapping("/resettle")
    public ResponseEntity<ApiResponse<List<SettlementDto>>> resettle(
            @RequestBody @jakarta.validation.Valid ResettleRequest request) {

        User currentUser = userContextHolder.getCurrentUser();
        log.info("재정산 요청: transactionEventId={}, user={}", request.getTransactionEventId(), currentUser.getUsername());

        try {
            List<Settlement> newSettlements = settlementResettlementService
                    .resettleByTransactionEventId(request.getTransactionEventId());

            List<SettlementDto> dtos = newSettlements.stream()
                    .map(SettlementDto::from)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(dtos));
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("재정산 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("RESETTLE_FAILED", e.getMessage()));
        }
    }

    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<SettlementBatchDto>> createBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "D_PLUS_1") SettlementCycle cycle) {

        log.info("Manual batch creation requested: date={}, cycle={}", date, cycle);

        SettlementBatch batch = settlementBatchService.createDailyBatch(date, cycle);
        if (batch == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        return ResponseEntity.ok(ApiResponse.success(SettlementBatchDto.from(batch)));
    }

    @PostMapping("/batches/realtime")
    public ResponseEntity<ApiResponse<SettlementBatchDto>> createRealtimeBatch() {
        log.info("Realtime batch creation requested");

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        SettlementBatch batch = settlementBatchService.createRealtimeBatch(today);
        if (batch == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        return ResponseEntity.ok(ApiResponse.success(SettlementBatchDto.from(batch)));
    }

    @GetMapping("/merchant-daily")
    public ResponseEntity<ApiResponse<List<DailySettlementSummaryDto>>> getMerchantDailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        var result = dailySettlementService.getDailySettlementSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/merchant-daily/{date}")
    public ResponseEntity<ApiResponse<DailySettlementDetailDto>> getMerchantDailyDetail(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var result = dailySettlementService.getDailySettlementDetail(date);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/merchant-statement/{merchantId}")
    public ResponseEntity<ApiResponse<MerchantStatementDto>> getMerchantStatement(
            @PathVariable UUID merchantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        var result = dailySettlementService.getMerchantStatement(merchantId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
