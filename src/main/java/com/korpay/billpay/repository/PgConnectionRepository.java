package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.domain.enums.PgConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PgConnectionRepository extends JpaRepository<PgConnection, UUID> {

    Optional<PgConnection> findByPgCode(String pgCode);

    Optional<PgConnection> findByWebhookPath(String webhookPath);

    List<PgConnection> findByStatus(PgConnectionStatus status);

    boolean existsByPgCode(String pgCode);

    boolean existsByWebhookPath(String webhookPath);
}
