package com.korpay.billpay.controller.platform;

import com.korpay.billpay.dto.request.TenantCreateRequest;
import com.korpay.billpay.dto.request.TenantUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.TenantResponse;
import com.korpay.billpay.service.platform.TenantManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/platform/tenants")
@RequiredArgsConstructor
public class TenantManagementController {

    private final TenantManagementService tenantService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TenantResponse>>> listTenants(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TenantResponse> tenants = tenantService.listTenants(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable String id) {
        TenantResponse tenant = tenantService.getTenant(id);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_OPERATOR')")
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody TenantCreateRequest request) {
        log.info("Creating tenant: {}", request.getTenantId());
        TenantResponse tenant = tenantService.createTenant(request);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_OPERATOR')")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable String id, @Valid @RequestBody TenantUpdateRequest request) {
        TenantResponse tenant = tenantService.updateTenant(id, request);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_OPERATOR')")
    public ResponseEntity<ApiResponse<TenantResponse>> suspendTenant(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        TenantResponse tenant = tenantService.suspendTenant(id, reason);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_OPERATOR')")
    public ResponseEntity<ApiResponse<TenantResponse>> activateTenant(@PathVariable String id) {
        TenantResponse tenant = tenantService.activateTenant(id);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }
}
