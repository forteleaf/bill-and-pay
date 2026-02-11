package com.korpay.billpay.service.platform;

import com.korpay.billpay.domain.entity.PlatformAnnouncement;
import com.korpay.billpay.domain.enums.AnnouncementStatus;
import com.korpay.billpay.repository.PlatformAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformAnnouncementService {

    private final PlatformAnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public Page<PlatformAnnouncement> list(AnnouncementStatus status, Pageable pageable) {
        if (status != null) {
            return announcementRepository.findByStatus(status, pageable);
        }
        return announcementRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public PlatformAnnouncement getById(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("공지사항을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public PlatformAnnouncement create(PlatformAnnouncement announcement) {
        return announcementRepository.save(announcement);
    }

    @Transactional
    public PlatformAnnouncement update(UUID id, PlatformAnnouncement updated) {
        PlatformAnnouncement existing = getById(id);
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setAnnouncementType(updated.getAnnouncementType());
        existing.setSeverity(updated.getSeverity());
        existing.setTargetType(updated.getTargetType());
        existing.setTargetTenantIds(updated.getTargetTenantIds());
        existing.setStatus(updated.getStatus());
        existing.setPublishAt(updated.getPublishAt());
        existing.setExpireAt(updated.getExpireAt());
        return announcementRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        PlatformAnnouncement announcement = getById(id);
        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        announcementRepository.save(announcement);
    }
}
