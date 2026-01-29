package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.CardCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardCompanyRepository extends JpaRepository<CardCompany, UUID> {

    Optional<CardCompany> findByCompanyCode(String companyCode);

    List<CardCompany> findByStatusOrderByDisplayOrderAsc(String status);
}
