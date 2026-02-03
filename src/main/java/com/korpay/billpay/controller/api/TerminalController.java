package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.TerminalStatus;
import com.korpay.billpay.domain.enums.TerminalType;
import com.korpay.billpay.dto.request.TerminalCreateRequest;
import com.korpay.billpay.dto.request.TerminalUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.dto.response.TerminalDto;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.terminal.TerminalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/terminals")
@RequiredArgsConstructor
@Validated
public class TerminalController {

    private final TerminalService terminalService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TerminalDto>>> listTerminals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TerminalStatus status,
            @RequestParam(required = false) TerminalType terminalType,
            @RequestParam(required = false) UUID merchantId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        User currentUser = userContextHolder.getCurrentUser();

        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<TerminalDto> terminalPage = terminalService.findTerminals(
                currentUser, status, terminalType, merchantId, organizationId, search, pageable);

        PagedResponse<TerminalDto> pagedResponse = PagedResponse.of(terminalPage, terminalPage.getContent());

        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TerminalDto>> getTerminal(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        TerminalDto terminal = terminalService.findById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(terminal));
    }

    @GetMapping("/tid/{tid}")
    public ResponseEntity<ApiResponse<TerminalDto>> getTerminalByTid(@PathVariable String tid) {
        User currentUser = userContextHolder.getCurrentUser();
        TerminalDto terminal = terminalService.findByTid(tid, currentUser);
        return ResponseEntity.ok(ApiResponse.success(terminal));
    }

    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<ApiResponse<List<TerminalDto>>> getTerminalsByMerchant(@PathVariable UUID merchantId) {
        User currentUser = userContextHolder.getCurrentUser();
        List<TerminalDto> terminals = terminalService.findByMerchant(merchantId, currentUser);
        return ResponseEntity.ok(ApiResponse.success(terminals));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TerminalDto>> createTerminal(
            @Valid @RequestBody TerminalCreateRequest request) {
        User currentUser = userContextHolder.getCurrentUser();
        TerminalDto terminal = terminalService.create(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(terminal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TerminalDto>> updateTerminal(
            @PathVariable UUID id,
            @Valid @RequestBody TerminalUpdateRequest request) {
        User currentUser = userContextHolder.getCurrentUser();
        TerminalDto terminal = terminalService.update(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success(terminal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTerminal(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        terminalService.delete(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
