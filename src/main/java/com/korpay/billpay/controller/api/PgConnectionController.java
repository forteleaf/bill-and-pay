package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.enums.PgConnectionStatus;
import com.korpay.billpay.dto.request.PgConnectionCreateRequest;
import com.korpay.billpay.dto.request.PgConnectionUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.dto.response.PgConnectionDto;
import com.korpay.billpay.service.pg.PgConnectionService;
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

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/pg-connections")
@RequiredArgsConstructor
@Validated
public class PgConnectionController {

    private final PgConnectionService pgConnectionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<PgConnectionDto>>> listConnections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "pgCode") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {

        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<PgConnectionDto> connectionsPage = pgConnectionService.findAll(pageable);

        PagedResponse<PgConnectionDto> pagedResponse = PagedResponse.of(connectionsPage, connectionsPage.getContent());

        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PgConnectionDto>> getConnection(@PathVariable UUID id) {
        PgConnectionDto dto = pgConnectionService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/code/{pgCode}")
    public ResponseEntity<ApiResponse<PgConnectionDto>> getConnectionByCode(@PathVariable String pgCode) {
        PgConnectionDto dto = pgConnectionService.findByPgCode(pgCode);
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PgConnectionDto>> createConnection(
            @Valid @RequestBody PgConnectionCreateRequest request) {

        PgConnectionDto dto = pgConnectionService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PgConnectionDto>> updateConnection(
            @PathVariable UUID id,
            @Valid @RequestBody PgConnectionUpdateRequest request) {

        PgConnectionDto dto = pgConnectionService.update(id, request);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PgConnectionDto>> updateConnectionStatus(
            @PathVariable UUID id,
            @RequestParam PgConnectionStatus status) {

        PgConnectionDto dto = pgConnectionService.updateStatus(id, status);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConnection(@PathVariable UUID id) {
        pgConnectionService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
