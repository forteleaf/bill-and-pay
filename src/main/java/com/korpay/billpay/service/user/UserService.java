package com.korpay.billpay.service.user;

import com.korpay.billpay.domain.entity.Organization;
import com.korpay.billpay.domain.entity.User;
import com.korpay.billpay.domain.enums.UserStatus;
import com.korpay.billpay.dto.request.UserCreateRequest;
import com.korpay.billpay.dto.request.UserUpdateRequest;
import com.korpay.billpay.exception.AccessDeniedException;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.OrganizationRepository;
import com.korpay.billpay.repository.UserRepository;
import com.korpay.billpay.service.auth.AccessControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessControlService accessControlService;

    /**
     * Create a new user in the specified organization.
     * 
     * @param request User creation request
     * @param currentUser The user making the request (for access control)
     * @return Created user
     * @throws EntityNotFoundException if organization not found
     * @throws ValidationException if username or email already exists
     * @throws AccessDeniedException if user lacks permission to create in target org
     */
    @Transactional
    public User create(UserCreateRequest request, User currentUser) {
        Organization organization = organizationRepository.findById(request.getOrgId())
                .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + request.getOrgId()));

        accessControlService.validateOrganizationAccess(currentUser, organization.getPath());

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ValidationException("Username already exists: " + request.getUsername());
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .organization(organization)
                .orgPath(organization.getPath())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .twoFactorEnabled(false)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    /**
     * Find user by ID.
     * 
     * @param id User ID
     * @return User
     * @throws EntityNotFoundException if user not found
     */
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    /**
     * Find user by ID with access control validation.
     * 
     * @param id User ID
     * @param currentUser The user making the request (for access control)
     * @return User
     * @throws EntityNotFoundException if user not found
     * @throws AccessDeniedException if user lacks permission to view target user
     */
    public User findById(UUID id, User currentUser) {
        User user = findById(id);
        accessControlService.validateOrganizationAccess(currentUser, user.getOrgPath());
        return user;
    }

    /**
     * Update user information.
     * 
     * @param id User ID
     * @param request Update request
     * @param currentUser The user making the request (for access control)
     * @return Updated user
     * @throws EntityNotFoundException if user not found
     * @throws AccessDeniedException if user lacks permission to update target user
     */
    @Transactional
    public User update(UUID id, UserUpdateRequest request, User currentUser) {
        User user = findById(id, currentUser);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        return userRepository.save(user);
    }

    /**
     * Soft delete user by setting deletedAt timestamp.
     * 
     * @param id User ID
     * @param currentUser The user making the request (for access control)
     * @throws EntityNotFoundException if user not found
     * @throws AccessDeniedException if user lacks permission to delete target user
     */
    @Transactional
    public void delete(UUID id, User currentUser) {
        User user = findById(id, currentUser);
        user.setDeletedAt(OffsetDateTime.now());
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    /**
     * Find all users in a specific organization.
     * 
     * @param orgId Organization ID
     * @param pageable Pagination info
     * @return Page of users
     */
    public Page<User> findByOrganization(UUID orgId, Pageable pageable) {
        List<User> users = userRepository.findByOrganizationId(orgId)
                .stream()
                .filter(u -> u.getDeletedAt() == null)
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), users.size());

        if (start > users.size()) {
            return new PageImpl<>(List.of(), pageable, users.size());
        }

        List<User> pageContent = users.subList(start, end);
        return new PageImpl<>(pageContent, pageable, users.size());
    }

    /**
     * Find all users accessible by the current user based on organization hierarchy.
     * Users can access all users in their org subtree.
     * 
     * @param currentUser The user making the request
     * @param pageable Pagination info
     * @return Page of accessible users
     */
    public Page<User> findAccessibleUsers(User currentUser, Pageable pageable) {
        List<User> accessibleUsers = userRepository.findByOrgPathDescendants(currentUser.getOrgPath())
                .stream()
                .filter(u -> u.getDeletedAt() == null)
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), accessibleUsers.size());

        if (start > accessibleUsers.size()) {
            return new PageImpl<>(List.of(), pageable, accessibleUsers.size());
        }

        List<User> pageContent = accessibleUsers.subList(start, end);
        return new PageImpl<>(pageContent, pageable, accessibleUsers.size());
    }

    /**
     * Change user password.
     * 
     * @param id User ID
     * @param oldPassword Current password (plain text)
     * @param newPassword New password (plain text)
     * @param currentUser The user making the request (for access control)
     * @throws EntityNotFoundException if user not found
     * @throws ValidationException if old password is incorrect
     * @throws AccessDeniedException if user lacks permission to change password
     */
    @Transactional
    public void changePassword(UUID id, String oldPassword, String newPassword, User currentUser) {
        User user = findById(id, currentUser);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(OffsetDateTime.now());
        userRepository.save(user);
    }
}
