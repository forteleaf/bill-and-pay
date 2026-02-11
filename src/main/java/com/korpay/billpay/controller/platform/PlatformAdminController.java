package com.korpay.billpay.controller.platform;

import com.korpay.billpay.domain.entity.PlatformAdmin;
import com.korpay.billpay.domain.enums.PlatformRole;
import com.korpay.billpay.dto.request.PlatformAdminCreateRequest;
import com.korpay.billpay.dto.request.PlatformAdminUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.service.platform.PlatformAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/platform/admins")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformAdminController {

    private final PlatformAdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlatformAdmin>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PlatformAdmin> admins = adminService.list(pageable);
        return ResponseEntity.ok(ApiResponse.success(admins));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformAdmin>> getById(@PathVariable UUID id) {
        PlatformAdmin admin = adminService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(admin));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlatformAdmin>> create(
            @Valid @RequestBody PlatformAdminCreateRequest request) {
        PlatformRole role = PlatformRole.valueOf(request.getRole());
        PlatformAdmin admin = adminService.create(
                request.getUsername(), request.getPassword(), request.getEmail(),
                request.getFullName(), request.getPhone(), role);
        return ResponseEntity.ok(ApiResponse.success(admin));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformAdmin>> update(
            @PathVariable UUID id, @Valid @RequestBody PlatformAdminUpdateRequest request) {
        PlatformRole role = request.getRole() != null ? PlatformRole.valueOf(request.getRole()) : null;
        PlatformAdmin admin = adminService.update(id, request.getEmail(),
                request.getFullName(), request.getPhone(), role);
        return ResponseEntity.ok(ApiResponse.success(admin));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("새 비밀번호는 필수입니다.");
        }
        adminService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspend(@PathVariable UUID id) {
        adminService.suspend(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable UUID id) {
        adminService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
