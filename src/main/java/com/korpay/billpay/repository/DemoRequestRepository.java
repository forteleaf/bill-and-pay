package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.DemoRequest;
import com.korpay.billpay.domain.enums.DemoRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface DemoRequestRepository extends JpaRepository<DemoRequest, UUID> {

    Page<DemoRequest> findByStatus(DemoRequestStatus status, Pageable pageable);

    @Query("SELECT COUNT(d) FROM DemoRequest d WHERE d.ipAddress = :ipAddress AND d.createdAt > :since")
    long countByIpAddressSince(@Param("ipAddress") String ipAddress, @Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(d) FROM DemoRequest d WHERE d.email = :email AND d.createdAt > :since")
    long countByEmailSince(@Param("email") String email, @Param("since") OffsetDateTime since);
}
