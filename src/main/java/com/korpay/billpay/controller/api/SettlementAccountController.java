package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.SettlementAccount;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.dto.request.SettlementAccountCreateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.SettlementAccountResponse;
import com.korpay.billpay.service.settlement.SettlementAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/settlement-accounts")
@RequiredArgsConstructor
@Validated
public class SettlementAccountController {

    private final SettlementAccountService settlementAccountService;

    @PostMapping
    public ResponseEntity<ApiResponse<SettlementAccountResponse>> createSettlementAccount(
            @Valid @RequestBody SettlementAccountCreateRequest request) {
        SettlementAccount account = settlementAccountService.create(request);
        SettlementAccountResponse response = SettlementAccountResponse.from(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SettlementAccountResponse>> getSettlementAccount(@PathVariable UUID id) {
        SettlementAccount account = settlementAccountService.findById(id);
        SettlementAccountResponse response = SettlementAccountResponse.from(account);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SettlementAccountResponse>> updateSettlementAccount(
            @PathVariable UUID id,
            @Valid @RequestBody SettlementAccountCreateRequest request) {
        SettlementAccount account = settlementAccountService.update(id, request);
        SettlementAccountResponse response = SettlementAccountResponse.from(account);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSettlementAccount(@PathVariable UUID id) {
        settlementAccountService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<List<SettlementAccountResponse>>> getSettlementAccountsByEntity(
            @PathVariable ContactEntityType entityType,
            @PathVariable UUID entityId) {
        List<SettlementAccount> accounts = settlementAccountService.findByEntity(entityType, entityId);
        List<SettlementAccountResponse> responses = accounts.stream()
                .map(SettlementAccountResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/entity/{entityType}/{entityId}/primary")
    public ResponseEntity<ApiResponse<SettlementAccountResponse>> getPrimarySettlementAccountByEntity(
            @PathVariable ContactEntityType entityType,
            @PathVariable UUID entityId) {
        SettlementAccount account = settlementAccountService.findPrimaryByEntity(entityType, entityId)
                .orElse(null);
        SettlementAccountResponse response = account != null ? SettlementAccountResponse.from(account) : null;
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/set-primary")
    public ResponseEntity<ApiResponse<SettlementAccountResponse>> setPrimarySettlementAccount(@PathVariable UUID id) {
        SettlementAccount account = settlementAccountService.setPrimary(id);
        SettlementAccountResponse response = SettlementAccountResponse.from(account);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<SettlementAccountResponse>> verifySettlementAccount(@PathVariable UUID id) {
        SettlementAccount account = settlementAccountService.verify(id);
        SettlementAccountResponse response = SettlementAccountResponse.from(account);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
