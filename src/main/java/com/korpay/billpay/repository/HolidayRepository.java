package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    boolean existsByHolidayDateAndCountryCode(LocalDate date, String countryCode);
}
