package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.PlatformAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLog, UUID> {
    Page<PlatformAuditLog> findByAdminId(UUID adminId, Pageable pageable);
    Page<PlatformAuditLog> findByAction(String action, Pageable pageable);
    Page<PlatformAuditLog> findByTargetTenantId(String tenantId, Pageable pageable);
    Page<PlatformAuditLog> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}
