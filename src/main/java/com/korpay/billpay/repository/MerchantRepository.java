package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByMerchantCode(String merchantCode);

    List<Merchant> findByOrganizationId(UUID organizationId);

    @Query(value = "SELECT * FROM merchants WHERE org_path <@ CAST(:path AS ltree)", nativeQuery = true)
    List<Merchant> findByOrgPathDescendants(@Param("path") String path);

    @Query(value = "SELECT * FROM merchants WHERE org_path ~ CAST(:lquery AS lquery)", nativeQuery = true)
    List<Merchant> findByOrgPathPattern(@Param("lquery") String lquery);

    @Query(value = "SELECT COUNT(*) FROM merchants WHERE org_path <@ CAST(:path AS ltree)", nativeQuery = true)
    long countByOrgPathDescendants(@Param("path") String path);

    @Query(value = "SELECT CAST(org_path AS text) as path, COUNT(*) as cnt FROM merchants GROUP BY org_path", nativeQuery = true)
    List<Object[]> countMerchantsByOrgPath();
}
