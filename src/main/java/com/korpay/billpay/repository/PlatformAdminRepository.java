package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.PlatformAdmin;
import com.korpay.billpay.domain.enums.PlatformAdminStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, UUID> {
    Optional<PlatformAdmin> findByUsername(String username);
    Optional<PlatformAdmin> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<PlatformAdmin> findByStatus(PlatformAdminStatus status, Pageable pageable);
}
