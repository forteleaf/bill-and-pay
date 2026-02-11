package com.korpay.billpay.controller.platform;

import com.korpay.billpay.domain.entity.PlatformAnnouncement;
import com.korpay.billpay.domain.enums.AnnouncementStatus;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.service.platform.PlatformAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/platform/announcements")
@RequiredArgsConstructor
public class PlatformAnnouncementController {

    private final PlatformAnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlatformAnnouncement>>> list(
            @RequestParam(required = false) AnnouncementStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PlatformAnnouncement> announcements = announcementService.list(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformAnnouncement>> getById(@PathVariable UUID id) {
        PlatformAnnouncement announcement = announcementService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(announcement));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_OPERATOR')")
    public ResponseEntity<ApiResponse<PlatformAnnouncement>> create(@RequestBody PlatformAnnouncement announcement) {
        PlatformAnnouncement created = announcementService.create(announcement);
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_OPERATOR')")
    public ResponseEntity<ApiResponse<PlatformAnnouncement>> update(
            @PathVariable UUID id, @RequestBody PlatformAnnouncement announcement) {
        PlatformAnnouncement updated = announcementService.update(id, announcement);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
