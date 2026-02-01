package com.korpay.billpay.service.organization;

import com.korpay.billpay.domain.entity.BusinessEntity;
import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.BusinessType;
import com.korpay.billpay.domain.enums.OrganizationStatus;
import com.korpay.billpay.domain.enums.OrganizationType;
import com.korpay.billpay.dto.request.OrganizationCreateRequest;
import com.korpay.billpay.dto.request.OrganizationUpdateRequest;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.BusinessEntityRepository;
import com.korpay.billpay.repository.OrganizationRepository;
import com.korpay.billpay.service.auth.AccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final BusinessEntityRepository businessEntityRepository;
    private final AccessControlService accessControlService;

    public Page<Organization> findAccessibleOrganizations(User user, Pageable pageable) {
        return findAccessibleOrganizations(user, pageable, null, null, null, null, null);
    }

    public Page<Organization> findAccessibleOrganizations(
            User user, 
            Pageable pageable,
            OrganizationType type,
            OrganizationStatus status,
            String search,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<Organization> allOrgs = organizationRepository.findAll();
        
        Stream<Organization> stream = allOrgs.stream()
                .filter(org -> accessControlService.hasAccessToOrganization(user, org.getPath()));
        
        if (type != null) {
            stream = stream.filter(org -> org.getOrgType() == type);
        }
        
        if (status != null) {
            stream = stream.filter(org -> org.getStatus() == status);
        }
        
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase().trim();
            stream = stream.filter(org -> {
                boolean matchOrgCode = org.getOrgCode() != null && 
                        org.getOrgCode().toLowerCase().contains(searchLower);
                boolean matchName = org.getName() != null && 
                        org.getName().toLowerCase().contains(searchLower);
                boolean matchBusinessName = org.getBusinessEntity() != null && 
                        org.getBusinessEntity().getBusinessName() != null &&
                        org.getBusinessEntity().getBusinessName().toLowerCase().contains(searchLower);
                boolean matchRepresentative = org.getBusinessEntity() != null && 
                        org.getBusinessEntity().getRepresentativeName() != null &&
                        org.getBusinessEntity().getRepresentativeName().toLowerCase().contains(searchLower);
                return matchOrgCode || matchName || matchBusinessName || matchRepresentative;
            });
        }
        
        if (startDate != null) {
            stream = stream.filter(org -> org.getCreatedAt() != null && 
                    !org.getCreatedAt().toLocalDate().isBefore(startDate));
        }
        if (endDate != null) {
            stream = stream.filter(org -> org.getCreatedAt() != null && 
                    !org.getCreatedAt().toLocalDate().isAfter(endDate));
        }
        
        List<Organization> accessibleOrgs = stream
                .sorted(Comparator.comparing(Organization::getCreatedAt, 
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), accessibleOrgs.size());
        
        if (start > accessibleOrgs.size()) {
            return new PageImpl<>(List.of(), pageable, accessibleOrgs.size());
        }
        
        List<Organization> pageContent = accessibleOrgs.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, accessibleOrgs.size());
    }

    public Organization findById(UUID id, User user) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + id));
        
        accessControlService.validateOrganizationAccess(user, organization);
        
        return organization;
    }

    public Organization findRoot(User user) {
        Organization root = organizationRepository.findByOrgType(OrganizationType.DISTRIBUTOR)
                .stream()
                .filter(org -> org.getLevel() == 1)
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Root organization not found"));
        
        accessControlService.validateOrganizationAccess(user, root);
        
        return root;
    }

    public List<Organization> findDescendants(UUID id, User user) {
        Organization organization = findById(id, user);
        
        return organizationRepository.findDescendants(organization.getPath());
    }

    @Transactional
    public Organization create(OrganizationCreateRequest request, User user) {
        Organization parent = null;
        String newPath;
        int newLevel;
        
        if (request.getParentId() != null) {
            parent = organizationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent organization not found: " + request.getParentId()));
            
            accessControlService.validateOrganizationCreation(user, parent.getPath());
            
            newPath = generatePath(parent, request.getOrgType());
            newLevel = parent.getLevel() + 1;
        } else {
            if (request.getOrgType() != com.korpay.billpay.domain.enums.OrganizationType.DISTRIBUTOR) {
                throw new ValidationException("Only DISTRIBUTOR can be created without parent");
            }
            
            String prefix = getPathPrefix(request.getOrgType());
            long count = organizationRepository.count() + 1;
            String segment = String.format("%s%03d", prefix, count);
            newPath = segment;
            newLevel = 1;
        }
        
        String orgCode = generateOrgCode(request.getOrgCode(), newPath);
        
        if (organizationRepository.findByOrgCode(orgCode).isPresent()) {
            throw new ValidationException("Organization code already exists: " + orgCode);
        }
        
        BusinessEntity businessEntity;
        if (request.getBusinessEntityId() != null) {
            businessEntity = businessEntityRepository.findById(request.getBusinessEntityId())
                    .orElseThrow(() -> new EntityNotFoundException("Business entity not found: " + request.getBusinessEntityId()));
        } else {
            businessEntity = BusinessEntity.builder()
                    .businessType(BusinessType.NON_BUSINESS)
                    .businessName(request.getName())
                    .representativeName("대표자")
                    .mainPhone(request.getPhone())
                    .email(request.getEmail())
                    .businessAddress(request.getAddress())
                    .build();
            businessEntity = businessEntityRepository.save(businessEntity);
        }
        
        Organization organization = Organization.builder()
                .orgCode(orgCode)
                .name(request.getName())
                .orgType(request.getOrgType())
                .path(newPath)
                .parent(parent)
                .level(newLevel)
                .businessEntity(businessEntity)
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
        
        if (request.getParentId() != null && !request.getParentId().equals(
                organization.getParent() != null ? organization.getParent().getId() : null)) {
            moveOrganization(organization, request.getParentId(), user);
        }
        
        if (request.getName() != null) {
            organization.setName(request.getName());
        }
        if (request.getBusinessEntityId() != null) {
            BusinessEntity businessEntity = businessEntityRepository.findById(request.getBusinessEntityId())
                    .orElseThrow(() -> new EntityNotFoundException("Business entity not found: " + request.getBusinessEntityId()));
            organization.setBusinessEntity(businessEntity);
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
    
    private void moveOrganization(Organization organization, UUID newParentId, User user) {
        if (organization.getOrgType() == OrganizationType.DISTRIBUTOR) {
            throw new ValidationException("Cannot move DISTRIBUTOR (root) organization");
        }
        
        Organization newParent = organizationRepository.findById(newParentId)
                .orElseThrow(() -> new EntityNotFoundException("New parent organization not found: " + newParentId));
        
        accessControlService.validateOrganizationAccess(user, newParent);
        
        int newParentTypeIndex = getTypeIndex(newParent.getOrgType());
        int orgTypeIndex = getTypeIndex(organization.getOrgType());
        if (newParentTypeIndex >= orgTypeIndex) {
            throw new ValidationException("Can only move to a higher-level organization type");
        }
        
        String oldPath = organization.getPath();
        if (newParent.getPath().startsWith(oldPath + ".") || newParent.getPath().equals(oldPath)) {
            throw new ValidationException("Cannot move organization under its own descendant");
        }
        
        String newPath = generatePath(newParent, organization.getOrgType());
        String newOrgCode = newPath.replace('.', '_');
        
        List<Organization> descendants = organizationRepository.findDescendants(oldPath);
        
        organization.setPath(newPath);
        organization.setOrgCode(newOrgCode);
        organization.setParent(newParent);
        organization.setLevel(newParent.getLevel() + 1);
        
        for (Organization descendant : descendants) {
            if (descendant.getId().equals(organization.getId())) continue;
            
            String descendantNewPath = descendant.getPath().replace(oldPath, newPath);
            String descendantNewOrgCode = descendantNewPath.replace('.', '_');
            
            descendant.setPath(descendantNewPath);
            descendant.setOrgCode(descendantNewOrgCode);
            descendant.setLevel(countPathSegments(descendantNewPath));
            
            organizationRepository.save(descendant);
        }
        
        log.info("Moved organization {} from {} to {}", organization.getId(), oldPath, newPath);
    }
    
    private int getTypeIndex(OrganizationType type) {
        return switch (type) {
            case DISTRIBUTOR -> 0;
            case AGENCY -> 1;
            case DEALER -> 2;
            case SELLER -> 3;
            case VENDOR -> 4;
        };
    }
    
    private int countPathSegments(String path) {
        return (int) path.chars().filter(c -> c == '.').count() + 1;
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
    
    private String generateOrgCode(String requestedCode, String path) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode;
        }
        
        return path.replace('.', '_');
    }
}
