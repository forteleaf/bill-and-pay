package com.korpay.billpay.service.organization;

import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.OrganizationStatus;
import com.korpay.billpay.dto.request.OrganizationCreateRequest;
import com.korpay.billpay.dto.request.OrganizationUpdateRequest;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.OrganizationRepository;
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
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final AccessControlService accessControlService;

    public Page<Organization> findAccessibleOrganizations(User user, Pageable pageable) {
        List<Organization> allOrgs = organizationRepository.findAll();
        
        List<Organization> accessibleOrgs = allOrgs.stream()
                .filter(org -> accessControlService.hasAccessToOrganization(user, org.getPath()))
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), accessibleOrgs.size());
        
        List<Organization> pageContent = accessibleOrgs.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, accessibleOrgs.size());
    }

    public Organization findById(UUID id, User user) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));
        
        accessControlService.validateOrganizationAccess(user, organization);
        
        return organization;
    }

    public List<Organization> findDescendants(UUID id, User user) {
        Organization organization = findById(id, user);
        
        return organizationRepository.findDescendants(organization.getPath());
    }

    @Transactional
    public Organization create(OrganizationCreateRequest request, User user) {
        Organization parent = organizationRepository.findById(request.getParentId())
                .orElseThrow(() -> new EntityNotFoundException("Parent organization not found: " + request.getParentId()));
        
        accessControlService.validateOrganizationCreation(user, parent.getPath());
        
        if (organizationRepository.findByOrgCode(request.getOrgCode()).isPresent()) {
            throw new ValidationException("Organization code already exists: " + request.getOrgCode());
        }
        
        String newPath = generatePath(parent, request.getOrgType());
        
        Organization organization = Organization.builder()
                .orgCode(request.getOrgCode())
                .name(request.getName())
                .orgType(request.getOrgType())
                .path(newPath)
                .parent(parent)
                .level(parent.getLevel() + 1)
                .businessNumber(request.getBusinessNumber())
                .businessName(request.getBusinessName())
                .representativeName(request.getRepresentativeName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .status(OrganizationStatus.ACTIVE)
                .config(request.getConfig())
                .build();
        
        return organizationRepository.save(organization);
    }

    @Transactional
    public Organization update(UUID id, OrganizationUpdateRequest request, User user) {
        Organization organization = findById(id, user);
        
        if (request.getName() != null) {
            organization.setName(request.getName());
        }
        if (request.getBusinessNumber() != null) {
            organization.setBusinessNumber(request.getBusinessNumber());
        }
        if (request.getBusinessName() != null) {
            organization.setBusinessName(request.getBusinessName());
        }
        if (request.getRepresentativeName() != null) {
            organization.setRepresentativeName(request.getRepresentativeName());
        }
        if (request.getEmail() != null) {
            organization.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            organization.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            organization.setAddress(request.getAddress());
        }
        if (request.getStatus() != null) {
            organization.setStatus(request.getStatus());
        }
        if (request.getConfig() != null) {
            organization.setConfig(request.getConfig());
        }
        
        return organizationRepository.save(organization);
    }

    private String generatePath(Organization parent, com.korpay.billpay.domain.enums.OrganizationType orgType) {
        String prefix = getPathPrefix(orgType);
        
        List<Organization> siblings = organizationRepository.findByParentId(parent.getId());
        long nextNumber = siblings.stream()
                .filter(s -> s.getPath().substring(s.getPath().lastIndexOf('.') + 1).startsWith(prefix))
                .count() + 1;
        
        String newSegment = String.format("%s%03d", prefix, nextNumber);
        
        return parent.getPath() + "." + newSegment;
    }

    private String getPathPrefix(com.korpay.billpay.domain.enums.OrganizationType orgType) {
        return switch (orgType) {
            case DISTRIBUTOR -> "dist_";
            case AGENCY -> "agcy_";
            case DEALER -> "deal_";
            case SELLER -> "sell_";
            case VENDOR -> "vend_";
        };
    }
}
