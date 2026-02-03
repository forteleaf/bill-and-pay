package com.korpay.billpay.service.terminal;

import com.korpay.billpay.domain.entity.Merchant;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.Terminal;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.TerminalStatus;
import com.korpay.billpay.domain.enums.TerminalType;
import com.korpay.billpay.dto.request.TerminalCreateRequest;
import com.korpay.billpay.dto.request.TerminalUpdateRequest;
import com.korpay.billpay.dto.response.TerminalDto;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.MerchantRepository;
import com.korpay.billpay.repository.OrganizationRepository;
import com.korpay.billpay.repository.TerminalRepository;
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
public class TerminalService {

    private final TerminalRepository terminalRepository;
    private final MerchantRepository merchantRepository;
    private final OrganizationRepository organizationRepository;
    private final AccessControlService accessControlService;

    public Page<TerminalDto> findTerminals(
            User user,
            TerminalStatus status,
            TerminalType terminalType,
            UUID merchantId,
            UUID organizationId,
            String search,
            Pageable pageable) {

        Page<Terminal> terminalPage = terminalRepository.findByFilters(
                status, terminalType, merchantId, organizationId, search, pageable);

        List<TerminalDto> accessibleTerminals = terminalPage.getContent().stream()
                .filter(terminal -> accessControlService.hasAccessToMerchant(user, terminal.getMerchant()))
                .map(TerminalDto::from)
                .collect(Collectors.toList());

        return new PageImpl<>(accessibleTerminals, pageable, terminalPage.getTotalElements());
    }

    public TerminalDto findById(UUID id, User user) {
        Terminal terminal = getTerminalOrThrow(id);
        accessControlService.validateMerchantAccess(user, terminal.getMerchant());
        return TerminalDto.from(terminal);
    }

    public TerminalDto findByTid(String tid, User user) {
        Terminal terminal = terminalRepository.findByTid(tid)
                .orElseThrow(() -> new EntityNotFoundException("단말기를 찾을 수 없습니다: " + tid));
        accessControlService.validateMerchantAccess(user, terminal.getMerchant());
        return TerminalDto.from(terminal);
    }

    public List<TerminalDto> findByMerchant(UUID merchantId, User user) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new EntityNotFoundException("가맹점을 찾을 수 없습니다: " + merchantId));
        accessControlService.validateMerchantAccess(user, merchant);

        return terminalRepository.findByMerchantId(merchantId).stream()
                .map(TerminalDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TerminalDto create(TerminalCreateRequest request, User user) {
        Merchant merchant = merchantRepository.findById(request.getMerchantId())
                .orElseThrow(() -> new EntityNotFoundException("가맹점을 찾을 수 없습니다: " + request.getMerchantId()));

        accessControlService.validateMerchantAccess(user, merchant);

        if (terminalRepository.existsByTid(request.getTid())) {
            throw new ValidationException("이미 존재하는 TID입니다: " + request.getTid());
        }

        if (request.getCatId() != null && terminalRepository.existsByCatId(request.getCatId())) {
            throw new ValidationException("이미 존재하는 CAT ID입니다: " + request.getCatId());
        }

        Organization organization = null;
        if (request.getOrganizationId() != null) {
            organization = organizationRepository.findById(request.getOrganizationId())
                    .orElseThrow(() -> new EntityNotFoundException("영업점을 찾을 수 없습니다: " + request.getOrganizationId()));
        }

        Terminal terminal = Terminal.builder()
                .tid(request.getTid())
                .catId(request.getCatId())
                .terminalType(request.getTerminalType())
                .merchant(merchant)
                .organization(organization)
                .serialNumber(request.getSerialNumber())
                .model(request.getModel())
                .manufacturer(request.getManufacturer())
                .installAddress(request.getInstallAddress())
                .installDate(request.getInstallDate())
                .status(TerminalStatus.ACTIVE)
                .config(request.getConfig())
                .build();

        Terminal saved = terminalRepository.save(terminal);
        log.info("Created terminal: {} ({})", saved.getTid(), saved.getId());

        return TerminalDto.from(saved);
    }

    @Transactional
    public TerminalDto update(UUID id, TerminalUpdateRequest request, User user) {
        Terminal terminal = getTerminalOrThrow(id);
        accessControlService.validateMerchantAccess(user, terminal.getMerchant());

        if (request.getCatId() != null) {
            if (!request.getCatId().equals(terminal.getCatId()) && 
                terminalRepository.existsByCatId(request.getCatId())) {
                throw new ValidationException("이미 존재하는 CAT ID입니다: " + request.getCatId());
            }
            terminal.setCatId(request.getCatId());
        }

        if (request.getSerialNumber() != null) {
            terminal.setSerialNumber(request.getSerialNumber());
        }
        if (request.getModel() != null) {
            terminal.setModel(request.getModel());
        }
        if (request.getManufacturer() != null) {
            terminal.setManufacturer(request.getManufacturer());
        }
        if (request.getInstallAddress() != null) {
            terminal.setInstallAddress(request.getInstallAddress());
        }
        if (request.getInstallDate() != null) {
            terminal.setInstallDate(request.getInstallDate());
        }
        if (request.getStatus() != null) {
            terminal.setStatus(request.getStatus());
        }
        if (request.getConfig() != null) {
            terminal.setConfig(request.getConfig());
        }

        Terminal saved = terminalRepository.save(terminal);
        log.info("Updated terminal: {} ({})", saved.getTid(), saved.getId());

        return TerminalDto.from(saved);
    }

    @Transactional
    public void delete(UUID id, User user) {
        Terminal terminal = getTerminalOrThrow(id);
        accessControlService.validateMerchantAccess(user, terminal.getMerchant());

        terminalRepository.delete(terminal);
        log.info("Deleted terminal: {} ({})", terminal.getTid(), id);
    }

    private Terminal getTerminalOrThrow(UUID id) {
        return terminalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("단말기를 찾을 수 없습니다: " + id));
    }
}
