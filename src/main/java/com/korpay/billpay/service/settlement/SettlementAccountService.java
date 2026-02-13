package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.entity.SettlementAccount;
import com.korpay.billpay.domain.enums.AccountStatus;
import com.korpay.billpay.domain.enums.ContactEntityType;
import com.korpay.billpay.dto.request.SettlementAccountCreateRequest;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.repository.SettlementAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementAccountService {

    private final SettlementAccountRepository settlementAccountRepository;

    @Transactional
    public SettlementAccount create(SettlementAccountCreateRequest request) {
        List<SettlementAccount> existing = settlementAccountRepository
                .findByAccountNumberAndBankCode(request.getAccountNumber(), request.getBankCode());
        boolean duplicateInEntity = existing.stream()
                .anyMatch(a -> a.getEntityType() == request.getEntityType()
                        && a.getEntityId().equals(request.getEntityId()));
        if (duplicateInEntity) {
            throw new IllegalArgumentException("동일한 은행의 계좌번호가 이미 등록되어 있습니다.");
        }

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            unsetPrimaryForEntity(request.getEntityType(), request.getEntityId());
        }

        SettlementAccount account = SettlementAccount.builder()
                .bankCode(request.getBankCode())
                .bankName(request.getBankName())
                .accountNumber(request.getAccountNumber())
                .accountHolder(request.getAccountHolder())
                .entityType(request.getEntityType())
                .entityId(request.getEntityId())
                .isPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false)
                .status(request.getStatus() != null ? request.getStatus() : AccountStatus.PENDING_VERIFICATION)
                .memo(request.getMemo())
                .build();

        log.info("Creating settlement account for entity: {} / {}", request.getEntityType(), request.getEntityId());
        return settlementAccountRepository.save(account);
    }

    public SettlementAccount findById(UUID id) {
        return settlementAccountRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("Settlement account not found: " + id));
    }

    @Transactional
    public SettlementAccount update(UUID id, SettlementAccountCreateRequest request) {
        SettlementAccount account = findById(id);

        List<SettlementAccount> existing = settlementAccountRepository
                .findByAccountNumberAndBankCode(request.getAccountNumber(), request.getBankCode());
        boolean duplicateInEntity = existing.stream()
                .anyMatch(a -> !a.getId().equals(id)
                        && a.getEntityType() == request.getEntityType()
                        && a.getEntityId().equals(request.getEntityId()));
        if (duplicateInEntity) {
            throw new IllegalArgumentException("동일한 은행의 계좌번호가 이미 등록되어 있습니다.");
        }

        if (Boolean.TRUE.equals(request.getIsPrimary()) && !account.getIsPrimary()) {
            unsetPrimaryForEntity(request.getEntityType(), request.getEntityId());
        }

        account.setBankCode(request.getBankCode());
        account.setBankName(request.getBankName());
        account.setAccountNumber(request.getAccountNumber());
        account.setAccountHolder(request.getAccountHolder());
        account.setEntityType(request.getEntityType());
        account.setEntityId(request.getEntityId());
        account.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : false);
        if (request.getStatus() != null) {
            account.setStatus(request.getStatus());
        }
        account.setMemo(request.getMemo());

        log.info("Updating settlement account: {}", id);
        return settlementAccountRepository.save(account);
    }

    @Transactional
    public void delete(UUID id) {
        SettlementAccount account = findById(id);
        account.softDelete();
        settlementAccountRepository.save(account);
        log.info("Soft deleted settlement account: {}", id);
    }

    public List<SettlementAccount> findByEntity(ContactEntityType entityType, UUID entityId) {
        return settlementAccountRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public Optional<SettlementAccount> findPrimaryByEntity(ContactEntityType entityType, UUID entityId) {
        return settlementAccountRepository.findPrimaryByEntityTypeAndEntityId(entityType, entityId);
    }

    public List<SettlementAccount> findByEntityAndStatus(ContactEntityType entityType, UUID entityId, AccountStatus status) {
        return settlementAccountRepository.findByEntityTypeAndEntityIdAndStatus(entityType, entityId, status);
    }

    @Transactional
    public SettlementAccount setPrimary(UUID id) {
        SettlementAccount account = findById(id);
        unsetPrimaryForEntity(account.getEntityType(), account.getEntityId());
        account.setIsPrimary(true);
        log.info("Set settlement account as primary: {}", id);
        return settlementAccountRepository.save(account);
    }

    @Transactional
    public SettlementAccount verify(UUID id) {
        SettlementAccount account = findById(id);
        account.verify();
        log.info("Verified settlement account: {}", id);
        return settlementAccountRepository.save(account);
    }

    @Transactional
    public SettlementAccount activate(UUID id) {
        SettlementAccount account = findById(id);
        account.activate();
        log.info("Activated settlement account: {}", id);
        return settlementAccountRepository.save(account);
    }

    @Transactional
    public SettlementAccount deactivate(UUID id) {
        SettlementAccount account = findById(id);
        account.deactivate();
        log.info("Deactivated settlement account: {}", id);
        return settlementAccountRepository.save(account);
    }

    @Transactional
    private void unsetPrimaryForEntity(ContactEntityType entityType, UUID entityId) {
        settlementAccountRepository.unsetPrimaryByEntityTypeAndEntityId(entityType, entityId);
    }
}
