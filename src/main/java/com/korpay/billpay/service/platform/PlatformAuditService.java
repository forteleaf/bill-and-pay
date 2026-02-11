package com.korpay.billpay.service.platform;

import com.korpay.billpay.domain.entity.PlatformAuditLog;
import com.korpay.billpay.repository.PlatformAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformAuditService {

    private final PlatformAuditLogRepository auditLogRepository;

    @Transactional
    public void log(UUID adminId, String adminUsername, String action,
                    String resourceType, String resourceId, String targetTenantId,
                    Map<String, Object> details, String ipAddress) {
        PlatformAuditLog auditLog = PlatformAuditLog.builder()
                .adminId(adminId)
                .adminUsername(adminUsername)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .targetTenantId(targetTenantId)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<PlatformAuditLog> listLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PlatformAuditLog> listLogsByTenant(String tenantId, Pageable pageable) {
        return auditLogRepository.findByTargetTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PlatformAuditLog> listLogsByAdmin(UUID adminId, Pageable pageable) {
        return auditLogRepository.findByAdminId(adminId, pageable);
    }
}
