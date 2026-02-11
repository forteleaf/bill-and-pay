package com.korpay.billpay.controller.platform;

import com.korpay.billpay.domain.entity.PlatformAuditLog;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.service.platform.PlatformAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/platform/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformAuditController {

    private final PlatformAuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlatformAuditLog>>> listLogs(
            @RequestParam(required = false) String tenantId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<PlatformAuditLog> logs;
        if (tenantId != null && !tenantId.isBlank()) {
            logs = auditService.listLogsByTenant(tenantId, pageable);
        } else {
            logs = auditService.listLogs(pageable);
        }
        return ResponseEntity.ok(ApiResponse.success(logs));
    }
}
