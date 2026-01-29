package com.korpay.billpay.service.merchant;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.MerchantStatus;
import com.korpay.billpay.domain.enums.TransactionStatus;
import com.korpay.billpay.dto.request.MerchantCreateRequest;
import com.korpay.billpay.dto.request.MerchantUpdateRequest;
import com.korpay.billpay.dto.response.MerchantStatisticsDto;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.OrganizationRepository;
import com.korpay.billpay.repository.TransactionRepository;
import com.korpay.billpay.service.auth.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final OrganizationRepository organizationRepository;
    private final TransactionRepository transactionRepository;
    private final AccessControlService accessControlService;

    public Page<Merchant> findAccessibleMerchants(User user, Pageable pageable) {
        List<Merchant> allMerchants = merchantRepository.findAll();
        
        List<Merchant> accessibleMerchants = allMerchants.stream()
                .filter(merchant -> accessControlService.hasAccessToMerchant(user, merchant))
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), accessibleMerchants.size());
        
        List<Merchant> pageContent = accessibleMerchants.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, accessibleMerchants.size());
    }

    public Merchant findById(UUID id, User user) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + id));
        
        accessControlService.validateMerchantAccess(user, merchant);
        
        return merchant;
    }

    @Transactional
    public Merchant create(MerchantCreateRequest request, User user) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + request.getOrganizationId()));
        
        accessControlService.validateOrganizationAccess(user, organization);
        
        if (merchantRepository.findByMerchantCode(request.getMerchantCode()).isPresent()) {
            throw new ValidationException("Merchant code already exists: " + request.getMerchantCode());
        }
        
        Merchant merchant = Merchant.builder()
                .merchantCode(request.getMerchantCode())
                .name(request.getName())
                .organization(organization)
                .orgPath(organization.getPath())
                .businessNumber(request.getBusinessNumber())
                .businessType(request.getBusinessType())
                .contactName(request.getContactName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .status(MerchantStatus.ACTIVE)
                .config(request.getConfig())
                .build();
        
        return merchantRepository.save(merchant);
    }

    @Transactional
    public Merchant update(UUID id, MerchantUpdateRequest request, User user) {
        Merchant merchant = findById(id, user);
        
        if (request.getName() != null) {
            merchant.setName(request.getName());
        }
        if (request.getBusinessNumber() != null) {
            merchant.setBusinessNumber(request.getBusinessNumber());
        }
        if (request.getBusinessType() != null) {
            merchant.setBusinessType(request.getBusinessType());
        }
        if (request.getContactName() != null) {
            merchant.setContactName(request.getContactName());
        }
        if (request.getContactEmail() != null) {
            merchant.setContactEmail(request.getContactEmail());
        }
        if (request.getContactPhone() != null) {
            merchant.setContactPhone(request.getContactPhone());
        }
        if (request.getAddress() != null) {
            merchant.setAddress(request.getAddress());
        }
        if (request.getStatus() != null) {
            merchant.setStatus(request.getStatus());
        }
        if (request.getConfig() != null) {
            merchant.setConfig(request.getConfig());
        }
        
        return merchantRepository.save(merchant);
    }

    public MerchantStatisticsDto getStatistics(UUID id, User user) {
        Merchant merchant = findById(id, user);
        
        List<com.korpay.billpay.domain.entity.Transaction> transactions = 
                transactionRepository.findByMerchantId(merchant.getId(), Pageable.unpaged()).getContent();
        
        long totalTransactions = transactions.size();
        long totalAmount = transactions.stream()
                .mapToLong(com.korpay.billpay.domain.entity.Transaction::getAmount)
                .sum();
        
        long approvedTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED)
                .count();
        long approvedAmount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.APPROVED)
                .mapToLong(com.korpay.billpay.domain.entity.Transaction::getAmount)
                .sum();
        
        long cancelledTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.CANCELLED)
                .count();
        long cancelledAmount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.CANCELLED)
                .mapToLong(com.korpay.billpay.domain.entity.Transaction::getAmount)
                .sum();
        
        long pendingTransactions = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING)
                .count();
        long pendingAmount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING)
                .mapToLong(com.korpay.billpay.domain.entity.Transaction::getAmount)
                .sum();
        
        return MerchantStatisticsDto.builder()
                .totalTransactions(totalTransactions)
                .totalAmount(totalAmount)
                .approvedTransactions(approvedTransactions)
                .approvedAmount(approvedAmount)
                .cancelledTransactions(cancelledTransactions)
                .cancelledAmount(cancelledAmount)
                .pendingTransactions(pendingTransactions)
                .pendingAmount(pendingAmount)
                .build();
    }
}
