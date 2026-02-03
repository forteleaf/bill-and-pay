package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.domain.enums.PgConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PgConnectionRepository extends JpaRepository<PgConnection, Long> {

    Optional<PgConnection> findByPgCode(String pgCode);

    List<PgConnection> findByStatus(PgConnectionStatus status);

    boolean existsByPgCode(String pgCode);

    Optional<PgConnection> findByIdAndTenantId(Long id, String tenantId);

    List<PgConnection> findByTenantId(String tenantId);
}
