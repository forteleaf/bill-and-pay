package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.SettlementAccount;
import com.korpay.billpay.domain.enums.AccountStatus;
import com.korpay.billpay.domain.enums.ContactEntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementAccountRepository extends JpaRepository<SettlementAccount, UUID> {

    @Query("SELECT sa FROM SettlementAccount sa WHERE sa.entityType = :entityType AND sa.entityId = :entityId AND sa.deletedAt IS NULL ORDER BY sa.isPrimary DESC, sa.createdAt ASC")
    List<SettlementAccount> findByEntityTypeAndEntityId(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId);

    @Query("SELECT sa FROM SettlementAccount sa WHERE sa.entityType = :entityType AND sa.entityId = :entityId AND sa.isPrimary = true AND sa.deletedAt IS NULL")
    Optional<SettlementAccount> findPrimaryByEntityTypeAndEntityId(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId);

    @Query("SELECT sa FROM SettlementAccount sa WHERE sa.entityType = :entityType AND sa.entityId = :entityId AND sa.status = :status AND sa.deletedAt IS NULL ORDER BY sa.isPrimary DESC, sa.createdAt ASC")
    List<SettlementAccount> findByEntityTypeAndEntityIdAndStatus(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId,
            @Param("status") AccountStatus status);

    @Query("SELECT sa FROM SettlementAccount sa WHERE sa.id = :id AND sa.deletedAt IS NULL")
    Optional<SettlementAccount> findByIdAndNotDeleted(@Param("id") UUID id);

    @Query("SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END FROM SettlementAccount sa WHERE sa.entityType = :entityType AND sa.entityId = :entityId AND sa.isPrimary = true AND sa.deletedAt IS NULL")
    boolean existsPrimaryByEntityTypeAndEntityId(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId);

    @Query("SELECT sa FROM SettlementAccount sa WHERE sa.accountNumber = :accountNumber AND sa.bankCode = :bankCode AND sa.deletedAt IS NULL")
    List<SettlementAccount> findByAccountNumberAndBankCode(
            @Param("accountNumber") String accountNumber,
            @Param("bankCode") String bankCode);

    @Query("SELECT sa FROM SettlementAccount sa WHERE sa.status = :status AND sa.deletedAt IS NULL ORDER BY sa.createdAt ASC")
    List<SettlementAccount> findByStatus(@Param("status") AccountStatus status);
}
