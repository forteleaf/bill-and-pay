package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByOrganizationId(UUID organizationId);

    @Query(value = "SELECT * FROM users WHERE org_path <@ CAST(:path AS ltree)", nativeQuery = true)
    List<User> findByOrgPathDescendants(@Param("path") String path);
}
