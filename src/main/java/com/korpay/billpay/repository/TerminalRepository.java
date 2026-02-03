package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Terminal;
import com.korpay.billpay.domain.enums.TerminalStatus;
import com.korpay.billpay.domain.enums.TerminalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, UUID> {

    Optional<Terminal> findByTid(String tid);

    Optional<Terminal> findByCatId(String catId);

    List<Terminal> findByMerchantId(UUID merchantId);

    List<Terminal> findByOrganizationId(UUID organizationId);

    Page<Terminal> findByStatus(TerminalStatus status, Pageable pageable);

    Page<Terminal> findByTerminalType(TerminalType terminalType, Pageable pageable);

    Page<Terminal> findByMerchantId(UUID merchantId, Pageable pageable);

    @Query("SELECT t FROM Terminal t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:terminalType IS NULL OR t.terminalType = :terminalType) AND " +
           "(:merchantId IS NULL OR t.merchant.id = :merchantId) AND " +
           "(:organizationId IS NULL OR t.organization.id = :organizationId) AND " +
           "(:search IS NULL OR t.tid LIKE %:search% OR t.catId LIKE %:search% OR t.merchant.name LIKE %:search%)")
    Page<Terminal> findByFilters(
            @Param("status") TerminalStatus status,
            @Param("terminalType") TerminalType terminalType,
            @Param("merchantId") UUID merchantId,
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable);

    boolean existsByTid(String tid);

    boolean existsByCatId(String catId);
}
