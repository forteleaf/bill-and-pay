package com.korpay.billpay.service.platform;

import com.korpay.billpay.domain.entity.PlatformAdmin;
import com.korpay.billpay.domain.enums.PlatformAdminStatus;
import com.korpay.billpay.domain.enums.PlatformRole;
import com.korpay.billpay.repository.PlatformAdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformAdminService {

    private final PlatformAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<PlatformAdmin> list(Pageable pageable) {
        return adminRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PlatformAdmin getById(UUID id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("관리자를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public PlatformAdmin create(String username, String password, String email,
                                String fullName, String phone, PlatformRole role) {
        if (adminRepository.existsByUsername(username)) {
            throw new RuntimeException("이미 존재하는 사용자명입니다: " + username);
        }
        if (adminRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 존재하는 이메일입니다: " + email);
        }

        PlatformAdmin admin = PlatformAdmin.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .phone(phone)
                .role(role)
                .build();
        return adminRepository.save(admin);
    }

    @Transactional
    public PlatformAdmin update(UUID id, String email, String fullName, String phone, PlatformRole role) {
        PlatformAdmin admin = getById(id);
        if (email != null) admin.setEmail(email);
        if (fullName != null) admin.setFullName(fullName);
        if (phone != null) admin.setPhone(phone);
        if (role != null) admin.setRole(role);
        return adminRepository.save(admin);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        PlatformAdmin admin = getById(id);
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setFailedLoginCount(0);
        admin.setLockedUntil(null);
        adminRepository.save(admin);
    }

    @Transactional
    public void suspend(UUID id) {
        PlatformAdmin admin = getById(id);
        admin.setStatus(PlatformAdminStatus.SUSPENDED);
        adminRepository.save(admin);
    }

    @Transactional
    public void activate(UUID id) {
        PlatformAdmin admin = getById(id);
        admin.setStatus(PlatformAdminStatus.ACTIVE);
        admin.setFailedLoginCount(0);
        admin.setLockedUntil(null);
        adminRepository.save(admin);
    }
}
