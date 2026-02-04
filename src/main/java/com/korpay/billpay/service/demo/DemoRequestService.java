package com.korpay.billpay.service.demo;

import com.korpay.billpay.domain.entity.DemoRequest;
import com.korpay.billpay.dto.request.DemoRequestCreateRequest;
import com.korpay.billpay.dto.response.DemoRequestResponse;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.DemoRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoRequestService {

    private final DemoRequestRepository demoRequestRepository;

    private static final int MAX_REQUESTS_PER_IP_PER_HOUR = 5;
    private static final int MAX_REQUESTS_PER_EMAIL_PER_DAY = 3;

    @Transactional
    public DemoRequestResponse createDemoRequest(
            DemoRequestCreateRequest request,
            String ipAddress,
            String userAgent) {
        
        validateRateLimit(request.getEmail(), ipAddress);

        String sanitizedPhone = request.getPhone().replaceAll("[^0-9]", "");

        DemoRequest entity = DemoRequest.builder()
                .companyName(sanitizeInput(request.getCompanyName()))
                .contactName(sanitizeInput(request.getContactName()))
                .email(request.getEmail().trim().toLowerCase())
                .phone(sanitizedPhone)
                .position(sanitizeInput(request.getPosition()))
                .employeeCount(request.getEmployeeCount())
                .monthlyVolume(request.getMonthlyVolume())
                .message(sanitizeInput(request.getMessage()))
                .ipAddress(ipAddress)
                .userAgent(truncate(userAgent, 500))
                .build();

        DemoRequest saved = demoRequestRepository.save(entity);
        
        log.info("Demo request created: id={}, company={}, email={}", 
                saved.getId(), saved.getCompanyName(), saved.getEmail());

        return DemoRequestResponse.from(saved);
    }

    private void validateRateLimit(String email, String ipAddress) {
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        long ipRequestCount = demoRequestRepository.countByIpAddressSince(ipAddress, oneHourAgo);
        
        if (ipRequestCount >= MAX_REQUESTS_PER_IP_PER_HOUR) {
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            throw new ValidationException("너무 많은 요청입니다. 잠시 후 다시 시도해주세요.");
        }

        OffsetDateTime oneDayAgo = OffsetDateTime.now().minusDays(1);
        long emailRequestCount = demoRequestRepository.countByEmailSince(email, oneDayAgo);
        
        if (emailRequestCount >= MAX_REQUESTS_PER_EMAIL_PER_DAY) {
            log.warn("Rate limit exceeded for email: {}", email);
            throw new ValidationException("이미 신청이 접수되었습니다. 담당자가 곧 연락드리겠습니다.");
        }
    }

    private String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim()
                .replaceAll("<[^>]*>", "")
                .replaceAll("[<>\"']", "");
    }

    private String truncate(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        return input.length() > maxLength ? input.substring(0, maxLength) : input;
    }
}
