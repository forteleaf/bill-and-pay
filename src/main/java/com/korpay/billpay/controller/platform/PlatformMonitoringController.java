package com.korpay.billpay.controller.platform;

import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.repository.PgConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/platform/monitoring")
@RequiredArgsConstructor
public class PlatformMonitoringController {

    private final PgConnectionRepository pgConnectionRepository;

    @GetMapping("/pg-status")
    public ResponseEntity<ApiResponse<List<PgConnection>>> getPgStatus() {
        List<PgConnection> connections = pgConnectionRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(connections));
    }
}
