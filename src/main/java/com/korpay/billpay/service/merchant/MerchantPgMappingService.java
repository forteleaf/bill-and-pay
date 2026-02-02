package com.korpay.billpay.service.merchant;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.MerchantPgMapping;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.MerchantPgMappingStatus;
import com.korpay.billpay.dto.request.MerchantPgMappingCreateRequest;
import com.korpay.billpay.dto.request.MerchantPgMappingUpdateRequest;
import com.korpay.billpay.dto.response.MerchantPgMappingDto;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.MerchantPgMappingRepository;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.service.auth.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
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
public class MerchantPgMappingService {

    private final MerchantPgMappingRepository merchantPgMappingRepository;
    private final MerchantRepository merchantRepository;
    private final AccessControlService accessControlService;

    public Page<MerchantPgMappingDto> findAll(User currentUser, Pageable pageable) {
        Page<MerchantPgMapping> mappingsPage = merchantPgMappingRepository.findAll(pageable);
        
        List<MerchantPgMappingDto> filteredMappings = mappingsPage.getContent().stream()
                .filter(mapping -> hasAccessToMapping(currentUser, mapping))
                .map(MerchantPgMappingDto::from)
                .collect(Collectors.toList());

        return new PageImpl<>(filteredMappings, pageable, filteredMappings.size());
    }

    public MerchantPgMappingDto findById(UUID id, User currentUser) {
        MerchantPgMapping mapping = getMappingOrThrow(id);
        accessControlService.validateMerchantAccess(currentUser, mapping.getMerchant().getOrgPath());
        return MerchantPgMappingDto.from(mapping);
    }

    public List<MerchantPgMappingDto> findByMerchantId(UUID merchantId, User currentUser) {
        Merchant merchant = getMerchantOrThrow(merchantId);
        accessControlService.validateMerchantAccess(currentUser, merchant.getOrgPath());

        return merchantPgMappingRepository.findByMerchantId(merchantId).stream()
                .map(MerchantPgMappingDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "merchantPgMappings", allEntries = true)
    public MerchantPgMappingDto create(MerchantPgMappingCreateRequest request, User currentUser) {
        Merchant merchant = getMerchantOrThrow(request.getMerchantId());
        accessControlService.validateMerchantAccess(currentUser, merchant.getOrgPath());

        validateUniqueMid(request.getMid(), request.getPgConnectionId(), null);

        MerchantPgMapping mapping = MerchantPgMapping.builder()
                .merchant(merchant)
                .pgConnectionId(request.getPgConnectionId())
                .mid(request.getMid())
                .terminalId(request.getTerminalId())
                .catId(request.getCatId())
                .config(request.getConfig())
                .status(request.getStatus() != null ? request.getStatus() : MerchantPgMappingStatus.ACTIVE)
                .build();

        MerchantPgMapping saved = merchantPgMappingRepository.save(mapping);
        log.info("Created PG mapping: {} for merchant: {}", saved.getId(), merchant.getId());

        return MerchantPgMappingDto.from(saved);
    }

    @Transactional
    @CacheEvict(value = "merchantPgMappings", allEntries = true)
    public MerchantPgMappingDto update(UUID id, MerchantPgMappingUpdateRequest request, User currentUser) {
        MerchantPgMapping mapping = getMappingOrThrow(id);
        accessControlService.validateMerchantAccess(currentUser, mapping.getMerchant().getOrgPath());

        if (request.getMid() != null && !request.getMid().equals(mapping.getMid())) {
            UUID pgConnectionId = request.getPgConnectionId() != null ? request.getPgConnectionId() : mapping.getPgConnectionId();
            validateUniqueMid(request.getMid(), pgConnectionId, id);
        }

        updateMappingFields(mapping, request);

        MerchantPgMapping saved = merchantPgMappingRepository.save(mapping);
        log.info("Updated PG mapping: {}", saved.getId());

        return MerchantPgMappingDto.from(saved);
    }

    @Transactional
    @CacheEvict(value = "merchantPgMappings", allEntries = true)
    public void delete(UUID id, User currentUser) {
        MerchantPgMapping mapping = getMappingOrThrow(id);
        accessControlService.validateMerchantAccess(currentUser, mapping.getMerchant().getOrgPath());

        merchantPgMappingRepository.delete(mapping);
        log.info("Deleted PG mapping: {}", id);
    }

    @Transactional
    @CacheEvict(value = "merchantPgMappings", allEntries = true)
    public MerchantPgMappingDto updateStatus(UUID id, MerchantPgMappingStatus status, User currentUser) {
        MerchantPgMapping mapping = getMappingOrThrow(id);
        accessControlService.validateMerchantAccess(currentUser, mapping.getMerchant().getOrgPath());

        mapping.setStatus(status);
        MerchantPgMapping saved = merchantPgMappingRepository.save(mapping);
        log.info("Updated PG mapping status: {} -> {}", id, status);

        return MerchantPgMappingDto.from(saved);
    }

    private MerchantPgMapping getMappingOrThrow(UUID id) {
        return merchantPgMappingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PG 매핑을 찾을 수 없습니다: " + id));
    }

    private Merchant getMerchantOrThrow(UUID merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("가맹점을 찾을 수 없습니다: " + merchantId));
    }

    private boolean hasAccessToMapping(User currentUser, MerchantPgMapping mapping) {
        try {
            accessControlService.validateMerchantAccess(currentUser, mapping.getMerchant().getOrgPath());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateUniqueMid(String mid, UUID pgConnectionId, UUID excludeId) {
        merchantPgMappingRepository.findByMidAndPgConnectionId(mid, pgConnectionId)
                .ifPresent(existing -> {
                    if (excludeId == null || !existing.getId().equals(excludeId)) {
                        throw new ValidationException("동일한 PG에 이미 등록된 MID입니다: " + mid);
                    }
                });
    }

    private void updateMappingFields(MerchantPgMapping mapping, MerchantPgMappingUpdateRequest request) {
        if (request.getPgConnectionId() != null) {
            mapping.setPgConnectionId(request.getPgConnectionId());
        }
        if (request.getMid() != null) {
            mapping.setMid(request.getMid());
        }
        if (request.getTerminalId() != null) {
            mapping.setTerminalId(request.getTerminalId());
        }
        if (request.getCatId() != null) {
            mapping.setCatId(request.getCatId());
        }
        if (request.getConfig() != null) {
            mapping.setConfig(request.getConfig());
        }
        if (request.getStatus() != null) {
            mapping.setStatus(request.getStatus());
        }
    }
}
