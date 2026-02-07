package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.WebhookIdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookIdempotencyKeyRepository extends JpaRepository<WebhookIdempotencyKey, WebhookIdempotencyKey.WebhookIdempotencyKeyId> {

    Optional<WebhookIdempotencyKey> findByPgConnectionIdAndPgTid(Long pgConnectionId, String pgTid);

    @Modifying
    @Query("UPDATE WebhookIdempotencyKey k SET k.status = :status, k.updatedAt = CURRENT_TIMESTAMP WHERE k.pgConnectionId = :pgConnectionId AND k.pgTid = :pgTid")
    int updateStatus(@Param("pgConnectionId") Long pgConnectionId, @Param("pgTid") String pgTid, @Param("status") String status);
}
