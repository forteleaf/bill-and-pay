package com.korpay.billpay.service.business;

import com.korpay.billpay.domain.entity.BusinessEntity;
import com.korpay.billpay.domain.enums.BusinessType;
import com.korpay.billpay.dto.request.BusinessEntityCreateRequest;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.BusinessEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessEntityService {

    private final BusinessEntityRepository businessEntityRepository;

    public Page<BusinessEntity> findAll(Pageable pageable) {
        return businessEntityRepository.findAll(pageable);
    }

    public BusinessEntity findById(UUID id) {
        return businessEntityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Business entity not found: " + id));
    }

    public Optional<BusinessEntity> findByBusinessNumber(String businessNumber) {
        return businessEntityRepository.findByBusinessNumber(businessNumber);
    }

    public List<BusinessEntity> findByBusinessType(BusinessType businessType) {
        return businessEntityRepository.findByBusinessType(businessType);
    }

    public List<BusinessEntity> searchByName(String name) {
        return businessEntityRepository.findByBusinessNameContaining(name);
    }

    @Transactional
    public BusinessEntity create(BusinessEntityCreateRequest request) {
        validateBusinessRequest(request);

        if (request.getBusinessNumber() != null && 
            businessEntityRepository.existsByBusinessNumber(request.getBusinessNumber())) {
            throw new ValidationException("이미 등록된 사업자번호입니다: " + request.getBusinessNumber());
        }

        BusinessEntity entity = BusinessEntity.builder()
                .businessType(request.getBusinessType())
                .businessNumber(request.getBusinessNumber())
                .corporateNumber(request.getCorporateNumber())
                .businessName(request.getBusinessName())
                .representativeName(request.getRepresentativeName())
                .openDate(request.getOpenDate())
                .businessAddress(request.getBusinessAddress())
                .actualAddress(request.getActualAddress())
                .businessCategory(request.getBusinessCategory())
                .businessSubCategory(request.getBusinessSubCategory())
                .mainPhone(request.getMainPhone())
                .managerName(request.getManagerName())
                .managerPhone(request.getManagerPhone())
                .email(request.getEmail())
                .build();

        return businessEntityRepository.save(entity);
    }

    @Transactional
    public BusinessEntity update(UUID id, BusinessEntityCreateRequest request) {
        BusinessEntity entity = findById(id);
        
        if (request.getBusinessNumber() != null && 
            !request.getBusinessNumber().equals(entity.getBusinessNumber()) &&
            businessEntityRepository.existsByBusinessNumber(request.getBusinessNumber())) {
            throw new ValidationException("이미 등록된 사업자번호입니다: " + request.getBusinessNumber());
        }

        validateBusinessRequest(request);

        entity.setBusinessType(request.getBusinessType());
        entity.setBusinessNumber(request.getBusinessNumber());
        entity.setCorporateNumber(request.getCorporateNumber());
        entity.setBusinessName(request.getBusinessName());
        entity.setRepresentativeName(request.getRepresentativeName());
        entity.setOpenDate(request.getOpenDate());
        entity.setBusinessAddress(request.getBusinessAddress());
        entity.setActualAddress(request.getActualAddress());
        entity.setBusinessCategory(request.getBusinessCategory());
        entity.setBusinessSubCategory(request.getBusinessSubCategory());
        entity.setMainPhone(request.getMainPhone());
        entity.setManagerName(request.getManagerName());
        entity.setManagerPhone(request.getManagerPhone());
        entity.setEmail(request.getEmail());

        return businessEntityRepository.save(entity);
    }

    private void validateBusinessRequest(BusinessEntityCreateRequest request) {
        if (request.getBusinessType() == BusinessType.NON_BUSINESS) {
            if (request.getBusinessNumber() != null) {
                throw new ValidationException("비사업자는 사업자번호를 가질 수 없습니다");
            }
        } else {
            if (request.getBusinessNumber() == null || request.getBusinessNumber().isBlank()) {
                throw new ValidationException("사업자/법인사업자는 사업자번호가 필수입니다");
            }
        }

        if (request.getBusinessType() == BusinessType.CORPORATION) {
            if (request.getCorporateNumber() == null || request.getCorporateNumber().isBlank()) {
                throw new ValidationException("법인사업자는 법인등록번호가 필수입니다");
            }
        }
    }
}
