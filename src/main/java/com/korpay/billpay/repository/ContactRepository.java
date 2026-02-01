package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Contact;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.domain.enums.ContactRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    @Query("SELECT c FROM Contact c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.deletedAt IS NULL ORDER BY c.isPrimary DESC, c.createdAt ASC")
    List<Contact> findByEntityTypeAndEntityId(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId);

    @Query("SELECT c FROM Contact c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.isPrimary = true AND c.deletedAt IS NULL")
    Optional<Contact> findPrimaryByEntityTypeAndEntityId(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId);

    @Query("SELECT c FROM Contact c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.role = :role AND c.deletedAt IS NULL")
    List<Contact> findByEntityTypeAndEntityIdAndRole(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId,
            @Param("role") ContactRole role);

    @Query("SELECT c FROM Contact c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Contact> findByIdAndNotDeleted(@Param("id") UUID id);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Contact c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.isPrimary = true AND c.deletedAt IS NULL")
    boolean existsPrimaryByEntityTypeAndEntityId(
            @Param("entityType") ContactEntityType entityType,
            @Param("entityId") UUID entityId);

    @Query("SELECT c FROM Contact c WHERE c.email = :email AND c.deletedAt IS NULL")
    List<Contact> findByEmail(@Param("email") String email);

    @Query("SELECT c FROM Contact c WHERE c.phone = :phone AND c.deletedAt IS NULL")
    List<Contact> findByPhone(@Param("phone") String phone);
}
