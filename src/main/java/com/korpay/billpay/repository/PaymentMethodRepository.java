package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.PaymentMethod;
import com.korpay.billpay.domain.enums.PaymentMethodCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    Optional<PaymentMethod> findByMethodCode(String methodCode);

    List<PaymentMethod> findByCategory(PaymentMethodCategory category);

    List<PaymentMethod> findByStatusOrderByDisplayOrderAsc(String status);
}
