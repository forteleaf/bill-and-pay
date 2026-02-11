package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, String> {
    Optional<Tenant> findBySchemaName(String schemaName);
    Page<Tenant> findByStatus(String status, Pageable pageable);
    boolean existsBySchemaName(String schemaName);
    long countByStatus(String status);
}
