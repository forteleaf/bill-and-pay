package com.korpay.billpay.controller.platform;

import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.PlatformDashboardResponse;
import com.korpay.billpay.service.platform.PlatformDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/platform/dashboard")
@RequiredArgsConstructor
public class PlatformDashboardController {

    private final PlatformDashboardService dashboardService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PlatformDashboardResponse>> getOverview() {
        PlatformDashboardResponse overview = dashboardService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }
}
