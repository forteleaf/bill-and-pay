package com.korpay.billpay.service.merchant;

import com.korpay.billpay.domain.entity.Contact;
import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.domain.enums.ContactRole;
import com.korpay.billpay.domain.enums.MerchantStatus;
import com.korpay.billpay.domain.enums.TransactionStatus;
import com.korpay.billpay.dto.request.MerchantCreateRequest;
import com.korpay.billpay.dto.request.MerchantUpdateRequest;
import com.korpay.billpay.dto.response.BlacklistCheckResponse;
import com.korpay.billpay.dto.response.MerchantDto;
import com.korpay.billpay.dto.response.MerchantStatisticsDto;
import com.korpay.billpay.dto.response.OrganizationDto;
import com.korpay.billpay.exception.DuplicateResourceException;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.domain.entity.MerchantOrgHistory;
import com.korpay.billpay.repository.ContactRepository;
import com.korpay.billpay.repository.MerchantOrgHistoryRepository;
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

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final MerchantOrgHistoryRepository merchantOrgHistoryRepository;
    private final ContactRepository contactRepository;
    private final AccessControlService accessControlService;
    private final BlacklistCheckService blacklistCheckService;

    public Page<Merchant> findAccessibleMerchants(User user, Pageable pageable) {
        if (accessControlService.isMasterAdmin(user)) {
            return merchantRepository.findAllNotDeleted(pageable);
        }
        return merchantRepository.findByOrgPathDescendants(user.getOrgPath(), pageable);
    }

    public Page<MerchantDto> findAccessibleMerchantsWithPrimaryContact(User user, Pageable pageable) {
        Page<Merchant> merchantPage = findAccessibleMerchants(user, pageable);

        List<UUID> merchantIds = merchantPage.getContent().stream()
                .map(Merchant::getId)
                .collect(Collectors.toList());

        Map<UUID, Contact> primaryContactMap = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            contactRepository.findPrimaryByEntityTypeAndEntityIds(ContactEntityType.MERCHANT, merchantIds)
                    .forEach(c -> primaryContactMap.put(c.getEntityId(), c));
        }

        List<MerchantDto> dtos = merchantPage.getContent().stream()
                .map(merchant -> MerchantDto.from(merchant, primaryContactMap.get(merchant.getId()), null))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, merchantPage.getTotalElements());
    }

    public Merchant findById(UUID id, User user) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Merchant not found: " + id));
        
        accessControlService.validateMerchantAccess(user, merchant);
        
        return merchant;
    }

    public MerchantDto findByIdWithContacts(UUID id, User user) {
        Merchant merchant = findById(id, user);
        Contact primaryContact = contactRepository.findPrimaryByEntityTypeAndEntityId(
                ContactEntityType.MERCHANT, merchant.getId()).orElse(null);
        List<Contact> contacts = contactRepository.findByEntityTypeAndEntityId(
                ContactEntityType.MERCHANT, merchant.getId());
        return MerchantDto.from(merchant, primaryContact, contacts);
    }

    public List<Contact> getMerchantContacts(UUID merchantId, User user) {
        Merchant merchant = findById(merchantId, user);
        return contactRepository.findByEntityTypeAndEntityId(ContactEntityType.MERCHANT, merchant.getId());
    }

    public Contact getPrimaryContact(UUID merchantId) {
        return contactRepository.findPrimaryByEntityTypeAndEntityId(
                ContactEntityType.MERCHANT, merchantId).orElse(null);
    }

    @Transactional
    public Merchant create(MerchantCreateRequest request, User user) {
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + request.getOrganizationId()));
        
        accessControlService.validateOrganizationAccess(user, organization);
        
        if (merchantRepository.findByMerchantCode(request.getMerchantCode()).isPresent()) {
            throw new DuplicateResourceException("Merchant code already exists: " + request.getMerchantCode());
        }
        
        Merchant merchant = Merchant.builder()
                .merchantCode(request.getMerchantCode())
                .name(request.getName())
                .organization(organization)
                .orgPath(organization.getPath())
                .businessNumber(request.getBusinessNumber())
                .businessType(request.getBusinessType())
                .corporateNumber(request.getCorporateNumber())
                .representativeName(request.getRepresentative())
                .address(request.getAddress())
                .status(MerchantStatus.ACTIVE)
                .config(request.getConfig())
                .build();
        
        Merchant savedMerchant = merchantRepository.save(merchant);
        
        if (request.getContactName() != null && !request.getContactName().isBlank()) {
            Contact primaryContact = Contact.builder()
                    .name(request.getContactName())
                    .phone(request.getContactPhone())
                    .email(request.getContactEmail())
                    .role(ContactRole.PRIMARY)
                    .entityType(ContactEntityType.MERCHANT)
                    .entityId(savedMerchant.getId())
                    .isPrimary(true)
                    .build();
            contactRepository.save(primaryContact);
        }
        
        return savedMerchant;
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
        if (request.getAddress() != null) {
            merchant.setAddress(request.getAddress());
        }
        if (request.getCorporateNumber() != null) {
            merchant.setCorporateNumber(request.getCorporateNumber());
        }
        if (request.getRepresentative() != null) {
            merchant.setRepresentativeName(request.getRepresentative());
        }
        if (request.getStatus() != null) {
            merchant.setStatus(request.getStatus());
        }
        if (request.getConfig() != null) {
            merchant.setConfig(request.getConfig());
        }
        
        if (request.getContactName() != null || request.getContactEmail() != null || request.getContactPhone() != null) {
            updatePrimaryContact(merchant.getId(), request);
        }
        
        return merchantRepository.save(merchant);
    }

    private void updatePrimaryContact(UUID merchantId, MerchantUpdateRequest request) {
        Optional<Contact> existingContact = contactRepository.findPrimaryByEntityTypeAndEntityId(
                ContactEntityType.MERCHANT, merchantId);
        
        if (existingContact.isPresent()) {
            Contact contact = existingContact.get();
            if (request.getContactName() != null) {
                contact.setName(request.getContactName());
            }
            if (request.getContactEmail() != null) {
                contact.setEmail(request.getContactEmail());
            }
            if (request.getContactPhone() != null) {
                contact.setPhone(request.getContactPhone());
            }
            contactRepository.save(contact);
        } else if (request.getContactName() != null && !request.getContactName().isBlank()) {
            Contact newContact = Contact.builder()
                    .name(request.getContactName())
                    .phone(request.getContactPhone())
                    .email(request.getContactEmail())
                    .role(ContactRole.PRIMARY)
                    .entityType(ContactEntityType.MERCHANT)
                    .entityId(merchantId)
                    .isPrimary(true)
                    .build();
            contactRepository.save(newContact);
        }
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

    @Transactional
    public Merchant moveMerchant(UUID merchantId, UUID targetOrgId, String reason, User user) {
        Merchant merchant = findById(merchantId, user);
        
        Organization targetOrg = organizationRepository.findById(targetOrgId)
                .orElseThrow(() -> new EntityNotFoundException("Target organization not found: " + targetOrgId));
        
        accessControlService.validateOrganizationAccess(user, targetOrg);
        
        if (merchant.getOrganization().getId().equals(targetOrgId)) {
            throw new ValidationException("Merchant is already in the target organization");
        }
        
        UUID fromOrgId = merchant.getOrganization().getId();
        String fromOrgPath = merchant.getOrgPath();
        
        MerchantOrgHistory history = MerchantOrgHistory.builder()
                .merchantId(merchant.getId())
                .fromOrgId(fromOrgId)
                .fromOrgPath(fromOrgPath)
                .toOrgId(targetOrgId)
                .toOrgPath(targetOrg.getPath())
                .movedBy(user.getUsername())
                .reason(reason)
                .build();
        
        merchantOrgHistoryRepository.save(history);
        
        merchant.setOrganization(targetOrg);
        merchant.setOrgPath(targetOrg.getPath());
        
        return merchantRepository.save(merchant);
    }

    public Page<MerchantOrgHistory> getMerchantHistory(UUID merchantId, User user, Pageable pageable) {
        Merchant merchant = findById(merchantId, user);
        return merchantOrgHistoryRepository.findByMerchantIdOrderByMovedAtDesc(merchant.getId(), pageable);
    }

    public List<OrganizationDto> getAccessibleOrganizations(User user) {
        List<Organization> allOrgs = organizationRepository.findAll();
        
        return allOrgs.stream()
                .filter(org -> accessControlService.hasAccessToOrganization(user, org.getPath()))
                .map(OrganizationDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(UUID id, User user) {
        Merchant merchant = findById(id, user);
        merchant.setStatus(MerchantStatus.DELETED);
        merchantRepository.delete(merchant);
        log.info("Soft deleted merchant: {} ({})", merchant.getMerchantCode(), id);
    }

    public BlacklistCheckResponse checkBlacklist(String businessNumber) {
        boolean isBlacklisted = blacklistCheckService.isBlacklisted(businessNumber);
        String reason = isBlacklisted ? blacklistCheckService.getReason(businessNumber) : null;

        return BlacklistCheckResponse.builder()
                .isBlacklisted(isBlacklisted)
                .reason(reason)
                .checkedAt(OffsetDateTime.now())
                .build();
    }
}
