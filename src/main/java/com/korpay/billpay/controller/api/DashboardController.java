package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.DashboardMetricsDto;
import com.korpay.billpay.dto.response.MerchantRankingDto;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserContextHolder userContextHolder;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<DashboardMetricsDto>> getMetrics() {
        User currentUser = userContextHolder.getCurrentUser();
        
        DashboardMetricsDto metrics = dashboardService.getMetrics(currentUser);
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @GetMapping("/top-merchants")
    public ResponseEntity<ApiResponse<List<MerchantRankingDto>>> getTopMerchants() {
        User currentUser = userContextHolder.getCurrentUser();
        
        List<MerchantRankingDto> rankings = dashboardService.getTopMerchants(currentUser, 5);
        
        return ResponseEntity.ok(ApiResponse.success(rankings));
    }
}
