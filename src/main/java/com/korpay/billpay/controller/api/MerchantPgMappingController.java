package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.MerchantPgMappingStatus;
import com.korpay.billpay.dto.request.MerchantPgMappingCreateRequest;
import com.korpay.billpay.dto.request.MerchantPgMappingUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.MerchantPgMappingDto;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.merchant.MerchantPgMappingService;
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

@Slf4j
@RestController
@RequestMapping("/v1/merchant-pg-mappings")
@RequiredArgsConstructor
@Validated
public class MerchantPgMappingController {

    private final MerchantPgMappingService merchantPgMappingService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<MerchantPgMappingDto>>> listMappings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<MerchantPgMappingDto> mappingsPage = merchantPgMappingService.findAll(currentUser, pageable);
        
        PagedResponse<MerchantPgMappingDto> pagedResponse = PagedResponse.of(mappingsPage, mappingsPage.getContent());
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantPgMappingDto>> getMapping(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        MerchantPgMappingDto dto = merchantPgMappingService.findById(id, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<MerchantPgMappingDto>>> getMappingsByMerchant(
            @PathVariable UUID merchantId) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        List<MerchantPgMappingDto> mappings = merchantPgMappingService.findByMerchantId(merchantId, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(mappings));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantPgMappingDto>> createMapping(
            @Valid @RequestBody MerchantPgMappingCreateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        MerchantPgMappingDto dto = merchantPgMappingService.create(request, currentUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantPgMappingDto>> updateMapping(
            @PathVariable UUID id,
            @Valid @RequestBody MerchantPgMappingUpdateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        MerchantPgMappingDto dto = merchantPgMappingService.update(id, request, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MerchantPgMappingDto>> updateMappingStatus(
            @PathVariable UUID id,
            @RequestParam MerchantPgMappingStatus status) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        MerchantPgMappingDto dto = merchantPgMappingService.updateStatus(id, status, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMapping(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        merchantPgMappingService.delete(id, currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
