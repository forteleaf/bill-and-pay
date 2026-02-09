package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.MerchantOrgHistory;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.dto.request.MerchantCreateRequest;
import com.korpay.billpay.dto.request.MerchantMoveRequest;
import com.korpay.billpay.dto.request.MerchantUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.BlacklistCheckResponse;
import com.korpay.billpay.dto.response.MerchantDto;
import com.korpay.billpay.dto.response.MerchantStatisticsDto;
import com.korpay.billpay.dto.response.OrganizationDto;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.merchant.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/merchants")
@RequiredArgsConstructor
@Validated
public class MerchantController {

    private final MerchantService merchantService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<MerchantDto>>> listMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<MerchantDto> merchantsPage = merchantService.findAccessibleMerchantsWithPrimaryContact(currentUser, pageable);
        
        PagedResponse<MerchantDto> pagedResponse = PagedResponse.of(merchantsPage, merchantsPage.getContent());
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantDto>> getMerchant(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        MerchantDto dto = merchantService.findByIdWithContacts(id, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<ApiResponse<MerchantStatisticsDto>> getMerchantStatistics(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        MerchantStatisticsDto statistics = merchantService.getStatistics(id, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantDto>> createMerchant(
            @Valid @RequestBody MerchantCreateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        Merchant merchant = merchantService.create(request, currentUser);
        MerchantDto dto = MerchantDto.from(merchant);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantDto>> updateMerchant(
            @PathVariable UUID id,
            @Valid @RequestBody MerchantUpdateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        Merchant merchant = merchantService.update(id, request, currentUser);
        MerchantDto dto = MerchantDto.from(merchant);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMerchant(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        merchantService.delete(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<ApiResponse<MerchantDto>> moveMerchant(
            @PathVariable UUID id,
            @Valid @RequestBody MerchantMoveRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        Merchant merchant = merchantService.moveMerchant(id, request.getTargetOrgId(), request.getReason(), currentUser);
        MerchantDto dto = MerchantDto.from(merchant);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<PagedResponse<MerchantOrgHistory>>> getMerchantHistory(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MerchantOrgHistory> historyPage = merchantService.getMerchantHistory(id, currentUser, pageable);
        
        PagedResponse<MerchantOrgHistory> pagedResponse = PagedResponse.of(historyPage, historyPage.getContent());
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/accessible-organizations")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getAccessibleOrganizations() {
        User currentUser = userContextHolder.getCurrentUser();
        
        List<OrganizationDto> organizations = merchantService.getAccessibleOrganizations(currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(organizations));
    }

    @GetMapping("/blacklist-check")
    public ResponseEntity<ApiResponse<BlacklistCheckResponse>> checkBlacklist(
            @RequestParam(value = "businessNumber") String businessNumber) {
        
        if (businessNumber == null || businessNumber.isBlank()) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("INVALID_REQUEST", "businessNumber parameter is required"));
        }
        
        BlacklistCheckResponse response = merchantService.checkBlacklist(businessNumber);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
