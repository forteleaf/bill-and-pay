package com.korpay.billpay.controller.api;

import com.korpay.billpay.dto.request.DemoRequestCreateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.DemoRequestResponse;
import com.korpay.billpay.service.demo.DemoRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/public/demo-requests")
@RequiredArgsConstructor
public class DemoRequestController {

    private final DemoRequestService demoRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<DemoRequestResponse>> createDemoRequest(
            @Valid @RequestBody DemoRequestCreateRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Demo request received from IP: {}, company: {}", ipAddress, request.getCompanyName());

        DemoRequestResponse response = demoRequestService.createDemoRequest(request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
