package com.korpay.billpay.controller.api;

import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.dto.request.OrganizationCreateRequest;
import com.korpay.billpay.dto.request.OrganizationUpdateRequest;
import com.korpay.billpay.dto.response.ApiResponse;
import com.korpay.billpay.dto.response.OrganizationDto;
import com.korpay.billpay.dto.response.PagedResponse;
import com.korpay.billpay.service.auth.UserContextHolder;
import com.korpay.billpay.service.organization.OrganizationService;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/v1/organizations")
@RequiredArgsConstructor
@Validated
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserContextHolder userContextHolder;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrganizationDto>>> listOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        if (size > 100) {
            size = 100;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<Organization> organizationsPage = organizationService.findAccessibleOrganizations(currentUser, pageable);
        
        List<OrganizationDto> dtos = organizationsPage.getContent().stream()
                .map(OrganizationDto::from)
                .collect(Collectors.toList());
        
        PagedResponse<OrganizationDto> pagedResponse = PagedResponse.of(organizationsPage, dtos);
        
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDto>> getOrganization(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        Organization organization = organizationService.findById(id, currentUser);
        OrganizationDto dto = OrganizationDto.from(organization);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @GetMapping("/{id}/descendants")
    public ResponseEntity<ApiResponse<List<OrganizationDto>>> getDescendants(@PathVariable UUID id) {
        User currentUser = userContextHolder.getCurrentUser();
        
        List<Organization> descendants = organizationService.findDescendants(id, currentUser);
        List<OrganizationDto> dtos = descendants.stream()
                .map(OrganizationDto::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationDto>> createOrganization(
            @Valid @RequestBody OrganizationCreateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        Organization organization = organizationService.create(request, currentUser);
        OrganizationDto dto = OrganizationDto.from(organization);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationDto>> updateOrganization(
            @PathVariable UUID id,
            @Valid @RequestBody OrganizationUpdateRequest request) {
        
        User currentUser = userContextHolder.getCurrentUser();
        
        Organization organization = organizationService.update(id, request, currentUser);
        OrganizationDto dto = OrganizationDto.from(organization);
        
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
