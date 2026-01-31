package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.BusinessEntity;
import com.korpay.billpay.domain.enums.BusinessType;
import com.korpay.billpay.dto.request.BusinessEntityCreateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.BusinessEntityResponse;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.service.business.BusinessEntityService;
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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/business-entities")
@RequiredArgsConstructor
@Validated
public class BusinessEntityController {

    private final BusinessEntityService businessEntityService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<BusinessEntityResponse>>> listBusinessEntities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) BusinessType businessType) {

        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<BusinessEntity> entityPage = businessEntityService.findAll(pageable);

        List<BusinessEntityResponse> dtos = entityPage.getContent().stream()
                .map(BusinessEntityResponse::from)
                .toList();

        PagedResponse<BusinessEntityResponse> pagedResponse = PagedResponse.of(entityPage, dtos);

        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessEntityResponse>> getBusinessEntity(@PathVariable UUID id) {
        BusinessEntity entity = businessEntityService.findById(id);
        BusinessEntityResponse dto = BusinessEntityResponse.from(entity);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<BusinessEntityResponse>> searchByBusinessNumber(
            @RequestParam String businessNumber) {

        Optional<BusinessEntity> entityOpt = businessEntityService.findByBusinessNumber(businessNumber);

        if (entityOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        BusinessEntityResponse dto = BusinessEntityResponse.from(entityOpt.get());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/search/name")
    public ResponseEntity<ApiResponse<List<BusinessEntityResponse>>> searchByName(
            @RequestParam String name) {

        List<BusinessEntity> entities = businessEntityService.searchByName(name);
        List<BusinessEntityResponse> dtos = entities.stream()
                .map(BusinessEntityResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/type/{businessType}")
    public ResponseEntity<ApiResponse<List<BusinessEntityResponse>>> getByBusinessType(
            @PathVariable BusinessType businessType) {

        List<BusinessEntity> entities = businessEntityService.findByBusinessType(businessType);
        List<BusinessEntityResponse> dtos = entities.stream()
                .map(BusinessEntityResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessEntityResponse>> createBusinessEntity(
            @Valid @RequestBody BusinessEntityCreateRequest request) {

        BusinessEntity entity = businessEntityService.create(request);
        BusinessEntityResponse dto = BusinessEntityResponse.from(entity);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessEntityResponse>> updateBusinessEntity(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessEntityCreateRequest request) {

        BusinessEntity entity = businessEntityService.update(id, request);
        BusinessEntityResponse dto = BusinessEntityResponse.from(entity);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
