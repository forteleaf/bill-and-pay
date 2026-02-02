package com.korpay.billpay.service.pg;

import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.domain.enums.PgConnectionStatus;
import com.korpay.billpay.dto.request.PgConnectionCreateRequest;
import com.korpay.billpay.dto.request.PgConnectionUpdateRequest;
import com.korpay.billpay.dto.response.PgConnectionDto;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.PgConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PgConnectionService {

    private final PgConnectionRepository pgConnectionRepository;

    public Page<PgConnectionDto> findAll(Pageable pageable) {
        return pgConnectionRepository.findAll(pageable)
                .map(PgConnectionDto::from);
    }

    public PgConnectionDto findById(UUID id) {
        PgConnection entity = getEntityOrThrow(id);
        return PgConnectionDto.from(entity);
    }

    public PgConnectionDto findByPgCode(String pgCode) {
        PgConnection entity = pgConnectionRepository.findByPgCode(pgCode)
                .orElseThrow(() -> new EntityNotFoundException("PG 연결을 찾을 수 없습니다: " + pgCode));
        return PgConnectionDto.from(entity);
    }

    @Transactional
    public PgConnectionDto create(PgConnectionCreateRequest request) {
        validateUniqueConstraints(request.getPgCode(), request.getWebhookPath(), null);

        PgConnection entity = PgConnection.builder()
                .pgCode(request.getPgCode())
                .pgName(request.getPgName())
                .pgApiVersion(request.getPgApiVersion())
                .merchantId(request.getMerchantId())
                .apiKeyEnc(encryptValue(request.getApiKey()))
                .apiSecretEnc(encryptValue(request.getApiSecret()))
                .webhookPath(request.getWebhookPath())
                .webhookSecret(request.getWebhookSecret())
                .apiBaseUrl(request.getApiBaseUrl())
                .apiEndpoints(request.getApiEndpoints())
                .status(PgConnectionStatus.ACTIVE)
                .build();

        PgConnection saved = pgConnectionRepository.save(entity);
        log.info("Created PG connection: {} ({})", saved.getPgCode(), saved.getId());

        return PgConnectionDto.from(saved);
    }

    @Transactional
    public PgConnectionDto update(UUID id, PgConnectionUpdateRequest request) {
        PgConnection entity = getEntityOrThrow(id);

        if (request.getPgName() != null) {
            entity.setPgName(request.getPgName());
        }
        if (request.getPgApiVersion() != null) {
            entity.setPgApiVersion(request.getPgApiVersion());
        }
        if (request.getApiKey() != null) {
            entity.setApiKeyEnc(encryptValue(request.getApiKey()));
        }
        if (request.getApiSecret() != null) {
            entity.setApiSecretEnc(encryptValue(request.getApiSecret()));
        }
        if (request.getWebhookSecret() != null) {
            entity.setWebhookSecret(request.getWebhookSecret());
        }
        if (request.getApiBaseUrl() != null) {
            entity.setApiBaseUrl(request.getApiBaseUrl());
        }
        if (request.getApiEndpoints() != null) {
            entity.setApiEndpoints(request.getApiEndpoints());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        PgConnection saved = pgConnectionRepository.save(entity);
        log.info("Updated PG connection: {} ({})", saved.getPgCode(), saved.getId());

        return PgConnectionDto.from(saved);
    }

    @Transactional
    public PgConnectionDto updateStatus(UUID id, PgConnectionStatus status) {
        PgConnection entity = getEntityOrThrow(id);
        entity.setStatus(status);

        PgConnection saved = pgConnectionRepository.save(entity);
        log.info("Updated PG connection status: {} -> {}", saved.getPgCode(), status);

        return PgConnectionDto.from(saved);
    }

    @Transactional
    public void delete(UUID id) {
        PgConnection entity = getEntityOrThrow(id);
        pgConnectionRepository.delete(entity);
        log.info("Deleted PG connection: {} ({})", entity.getPgCode(), id);
    }

    private PgConnection getEntityOrThrow(UUID id) {
        return pgConnectionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("PG 연결을 찾을 수 없습니다: " + id));
    }

    private void validateUniqueConstraints(String pgCode, String webhookPath, UUID excludeId) {
        pgConnectionRepository.findByPgCode(pgCode)
                .ifPresent(existing -> {
                    if (excludeId == null || !existing.getId().equals(excludeId)) {
                        throw new ValidationException("이미 존재하는 PG 코드입니다: " + pgCode);
                    }
                });

        pgConnectionRepository.findByWebhookPath(webhookPath)
                .ifPresent(existing -> {
                    if (excludeId == null || !existing.getId().equals(excludeId)) {
                        throw new ValidationException("이미 사용 중인 Webhook 경로입니다: " + webhookPath);
                    }
                });
    }

    private byte[] encryptValue(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
