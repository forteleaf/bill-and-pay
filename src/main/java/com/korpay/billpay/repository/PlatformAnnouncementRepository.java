package com.korpay.billpay.repository;

import com.korpay.billpay.domain.entity.PlatformAnnouncement;
import com.korpay.billpay.domain.enums.AnnouncementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PlatformAnnouncementRepository extends JpaRepository<PlatformAnnouncement, UUID> {
    Page<PlatformAnnouncement> findByStatus(AnnouncementStatus status, Pageable pageable);
    Page<PlatformAnnouncement> findByStatusOrderByCreatedAtDesc(AnnouncementStatus status, Pageable pageable);
}
