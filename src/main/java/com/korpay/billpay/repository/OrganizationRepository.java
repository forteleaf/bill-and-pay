package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.enums.OrganizationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByOrgCode(String orgCode);

    @Query(value = "SELECT * FROM organizations WHERE path <@ CAST(:path AS public.ltree)", nativeQuery = true)
    List<Organization> findDescendants(@Param("path") String path);

    @Query(value = "SELECT * FROM organizations WHERE CAST(:path AS public.ltree) <@ path", nativeQuery = true)
    List<Organization> findAncestors(@Param("path") String path);

    @Query(value = "SELECT * FROM organizations WHERE path ~ CAST(:lquery AS lquery)", nativeQuery = true)
    List<Organization> findByPathPattern(@Param("lquery") String lquery);

    List<Organization> findByOrgType(OrganizationType orgType);

    List<Organization> findByParentId(UUID parentId);

    @Query(value = "SELECT * FROM organizations WHERE nlevel(path) = :level", nativeQuery = true)
    List<Organization> findByLevel(@Param("level") int level);
}
