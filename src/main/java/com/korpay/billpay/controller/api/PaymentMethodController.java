package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.PaymentMethod;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.service.FeeConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final FeeConfigurationService feeConfigurationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentMethodResponse>>> list() {
        List<PaymentMethod> methods = feeConfigurationService.getActivePaymentMethods();
        List<PaymentMethodResponse> responses = methods.stream()
                .map(pm -> new PaymentMethodResponse(pm.getId(), pm.getMethodCode(), pm.getName(), pm.getCategory().name()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    public record PaymentMethodResponse(UUID id, String methodCode, String name, String category) {}
}
