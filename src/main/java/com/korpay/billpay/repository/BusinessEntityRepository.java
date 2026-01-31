package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.BusinessEntity;
import com.korpay.billpay.domain.enums.BusinessType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessEntityRepository extends JpaRepository<BusinessEntity, UUID> {

    Optional<BusinessEntity> findByBusinessNumber(String businessNumber);

    boolean existsByBusinessNumber(String businessNumber);

    List<BusinessEntity> findByBusinessType(BusinessType businessType);

    List<BusinessEntity> findByBusinessNameContaining(String businessName);

    List<BusinessEntity> findByRepresentativeNameContaining(String representativeName);
}
