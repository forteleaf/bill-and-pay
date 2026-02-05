package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.FeeConfiguration;
import com.korpay.billpay.domain.enums.FeeConfigStatus;
import com.korpay.billpay.domain.enums.OrganizationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeConfigurationRepository extends JpaRepository<FeeConfiguration, UUID> {

    @Query("""
            SELECT fc FROM FeeConfiguration fc
            WHERE fc.entityId = :entityId
            AND fc.entityType = :entityType
            AND fc.paymentMethodId = :paymentMethodId
            AND fc.status = :status
            AND fc.validFrom <= :now
            AND (fc.validUntil IS NULL OR fc.validUntil > :now)
            ORDER BY fc.priority ASC
            """)
    List<FeeConfiguration> findActiveByEntityAndPaymentMethod(
            @Param("entityId") UUID entityId,
            @Param("entityType") OrganizationType entityType,
            @Param("paymentMethodId") UUID paymentMethodId,
            @Param("status") FeeConfigStatus status,
            @Param("now") OffsetDateTime now);

    @Query("""
            SELECT fc FROM FeeConfiguration fc
            WHERE fc.entityId = :entityId
            AND fc.entityType = :entityType
            AND fc.status = :status
            AND fc.validFrom <= :now
            AND (fc.validUntil IS NULL OR fc.validUntil > :now)
            ORDER BY fc.priority ASC
            """)
    List<FeeConfiguration> findActiveByEntity(
            @Param("entityId") UUID entityId,
            @Param("entityType") OrganizationType entityType,
            @Param("status") FeeConfigStatus status,
            @Param("now") OffsetDateTime now);

    Optional<FeeConfiguration> findByEntityIdAndEntityTypeAndPaymentMethodIdAndStatus(
            UUID entityId, OrganizationType entityType, UUID paymentMethodId, FeeConfigStatus status);

    List<FeeConfiguration> findByEntityIdAndStatus(UUID entityId, FeeConfigStatus status);
}
