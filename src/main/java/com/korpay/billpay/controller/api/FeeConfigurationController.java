package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.FeeConfigHistory;
import com.korpay.billpay.domain.entity.FeeConfiguration;
import com.korpay.billpay.dto.request.FeeConfigurationCreateRequest;
import com.korpay.billpay.dto.request.FeeConfigurationUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.FeeConfigHistoryResponse;
import com.korpay.billpay.dto.response.FeeConfigurationResponse;
import com.korpay.billpay.service.FeeConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/fee-configurations")
@RequiredArgsConstructor
@Validated
public class FeeConfigurationController {

    private final FeeConfigurationService feeConfigurationService;

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<FeeConfigurationResponse>>> listByMerchant(
            @PathVariable UUID merchantId) {
        List<FeeConfiguration> configs = feeConfigurationService.listByMerchant(merchantId);
        List<FeeConfigurationResponse> responses = configs.stream()
                .map(fc -> FeeConfigurationResponse.from(fc,
                        feeConfigurationService.resolvePaymentMethodName(fc.getPaymentMethodId())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeConfigurationResponse>> getById(@PathVariable UUID id) {
        FeeConfiguration config = feeConfigurationService.getById(id);
        FeeConfigurationResponse response = FeeConfigurationResponse.from(config,
                feeConfigurationService.resolvePaymentMethodName(config.getPaymentMethodId()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<FeeConfigurationResponse>> create(
            @PathVariable UUID merchantId,
            @Valid @RequestBody FeeConfigurationCreateRequest request) {
        FeeConfiguration config = feeConfigurationService.create(merchantId, request);
        FeeConfigurationResponse response = FeeConfigurationResponse.from(config,
                feeConfigurationService.resolvePaymentMethodName(config.getPaymentMethodId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeConfigurationResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody FeeConfigurationUpdateRequest request) {
        FeeConfiguration config = feeConfigurationService.update(id, request);
        FeeConfigurationResponse response = FeeConfigurationResponse.from(config,
                feeConfigurationService.resolvePaymentMethodName(config.getPaymentMethodId()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        feeConfigurationService.deactivate(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        feeConfigurationService.activate(id, reason);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<FeeConfigHistoryResponse>>> getHistory(
            @PathVariable UUID id) {
        List<FeeConfigHistory> history = feeConfigurationService.getHistory(id);
        List<FeeConfigHistoryResponse> responses = history.stream()
                .map(FeeConfigHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/merchant/{merchantId}/history")
    public ResponseEntity<ApiResponse<List<FeeConfigHistoryResponse>>> getMerchantHistory(
            @PathVariable UUID merchantId) {
        List<FeeConfigHistory> history = feeConfigurationService.getMerchantHistory(merchantId);
        List<FeeConfigHistoryResponse> responses = history.stream()
                .map(FeeConfigHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
