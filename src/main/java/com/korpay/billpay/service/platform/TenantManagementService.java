package com.korpay.billpay.service.platform;

import com.korpay.billpay.domain.entity.AuthUser;
import com.korpay.billpay.domain.entity.Tenant;
import com.korpay.billpay.dto.request.TenantCreateRequest;
import com.korpay.billpay.dto.request.TenantUpdateRequest;
import com.korpay.billpay.dto.response.TenantResponse;
import com.korpay.billpay.repository.AuthUserRepository;
import com.korpay.billpay.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantManagementService {

    private final TenantRepository tenantRepository;
    private final AuthUserRepository authUserRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<TenantResponse> listTenants(String status, Pageable pageable) {
        Page<Tenant> tenants;
        if (status != null && !status.isBlank()) {
            tenants = tenantRepository.findByStatus(status, pageable);
        } else {
            tenants = tenantRepository.findAll(pageable);
        }
        return tenants.map(TenantResponse::from);
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(String id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("테넌트를 찾을 수 없습니다: " + id));
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse createTenant(TenantCreateRequest request) {
        if (tenantRepository.existsById(request.getTenantId())) {
            throw new RuntimeException("이미 존재하는 테넌트 ID입니다: " + request.getTenantId());
        }

        String schemaName = "tenant_" + request.getTenantId();
        if (tenantRepository.existsBySchemaName(schemaName)) {
            throw new RuntimeException("이미 존재하는 스키마명입니다: " + schemaName);
        }

        // 1. 테넌트 레코드 생성 (PROVISIONING 상태)
        Tenant tenant = Tenant.builder()
                .id(request.getTenantId())
                .name(request.getName())
                .schemaName(schemaName)
                .status("PROVISIONING")
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();
        tenantRepository.save(tenant);

        try {
            // 2. 스키마 생성
            jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
            log.info("Created schema: {}", schemaName);

            // 3. AuthUser 생성 (public.users)
            AuthUser authUser = AuthUser.builder()
                    .username(request.getAdminUsername())
                    .password(passwordEncoder.encode(request.getAdminPassword()))
                    .tenantId(request.getTenantId())
                    .status("ACTIVE")
                    .build();
            authUserRepository.save(authUser);

            // 4. 상태를 ACTIVE로 변경
            tenant.setStatus("ACTIVE");
            tenantRepository.save(tenant);

            log.info("Tenant provisioned successfully: {}", request.getTenantId());
        } catch (Exception e) {
            log.error("Tenant provisioning failed: {}", request.getTenantId(), e);
            tenant.setStatus("PROVISIONING");
            tenantRepository.save(tenant);
            throw new RuntimeException("테넌트 프로비저닝 실패: " + e.getMessage(), e);
        }

        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse updateTenant(String id, TenantUpdateRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("테넌트를 찾을 수 없습니다: " + id));

        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getContactEmail() != null) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            tenant.setContactPhone(request.getContactPhone());
        }

        tenantRepository.save(tenant);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse suspendTenant(String id, String reason) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("테넌트를 찾을 수 없습니다: " + id));

        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new RuntimeException("활성 상태의 테넌트만 정지할 수 있습니다.");
        }

        tenant.setStatus("SUSPENDED");
        tenantRepository.save(tenant);

        log.info("Tenant suspended: {} (reason: {})", id, reason);
        return TenantResponse.from(tenant);
    }

    @Transactional
    public TenantResponse activateTenant(String id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("테넌트를 찾을 수 없습니다: " + id));

        if (!"SUSPENDED".equals(tenant.getStatus())) {
            throw new RuntimeException("정지 상태의 테넌트만 활성화할 수 있습니다.");
        }

        tenant.setStatus("ACTIVE");
        tenantRepository.save(tenant);

        log.info("Tenant activated: {}", id);
        return TenantResponse.from(tenant);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return tenantRepository.countByStatus(status);
    }
}
