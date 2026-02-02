package com.korpay.billpay.service.pg;

import com.korpay.billpay.domain.entity.PgConnection;
import com.korpay.billpay.domain.enums.PgConnectionStatus;
import com.korpay.billpay.dto.request.PgConnectionCreateRequest;
import com.korpay.billpay.dto.request.PgConnectionUpdateRequest;
import com.korpay.billpay.dto.response.PgConnectionDto;
import com.korpay.billpay.exception.EntityNotFoundException;
import com.korpay.billpay.exception.ValidationException;
import com.korpay.billpay.repository.PgConnectionRepository;
import com.korpay.billpay.service.webhook.WebhookUrlGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PgConnectionService {

    private final PgConnectionRepository pgConnectionRepository;
    private final WebhookUrlGenerator webhookUrlGenerator;

    public Page<PgConnectionDto> findAll(Pageable pageable) {
        return pgConnectionRepository.findAll(pageable)
                .map(this::toDto);
    }

    public PgConnectionDto findById(UUID id) {
        PgConnection entity = getEntityOrThrow(id);
        return toDto(entity);
    }

    public PgConnectionDto findByPgCode(String pgCode) {
        PgConnection entity = pgConnectionRepository.findByPgCode(pgCode)
                .orElseThrow(() -> new EntityNotFoundException("PG 연결을 찾을 수 없습니다: " + pgCode));
        return toDto(entity);
    }

    private PgConnectionDto toDto(PgConnection entity) {
        String generatedWebhookUrl = null;
        String legacyWebhookUrl = null;

        if (entity.getTenantId() != null && entity.getWebhookSecret() != null) {
            generatedWebhookUrl = webhookUrlGenerator.generateNewUrl(
                    entity.getTenantId(),
                    entity.getPgCode(),
                    entity.getId(),
                    entity.getWebhookSecret()
            );
        }

        if (entity.getWebhookSecret() != null) {
            legacyWebhookUrl = webhookUrlGenerator.generateLegacyUrl(
                    entity.getPgCode(),
                    entity.getId(),
                    entity.getWebhookSecret()
            );
        }

        return PgConnectionDto.from(entity, generatedWebhookUrl, legacyWebhookUrl);
    }

    @Transactional
    public PgConnectionDto create(PgConnectionCreateRequest request) {
        validateUniqueConstraints(request.getPgCode(), null);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("api_key", request.getApiKey());
        credentials.put("secret_key", request.getSecretKey());
        credentials.put("merchant_id", request.getMerchantId());

        Map<String, Object> config = new HashMap<>();
        if (request.getWebhookSecret() != null) {
            config.put("webhook_secret", request.getWebhookSecret());
        }
        if (request.getTimeoutMs() != null) {
            config.put("timeout_ms", request.getTimeoutMs());
        }
        if (request.getRetryCount() != null) {
            config.put("retry_count", request.getRetryCount());
        }

        PgConnection entity = PgConnection.builder()
                .pgCode(request.getPgCode())
                .pgName(request.getPgName())
                .apiBaseUrl(request.getApiBaseUrl())
                .webhookBaseUrl(request.getWebhookBaseUrl())
                .credentials(credentials)
                .config(config.isEmpty() ? null : config)
                .status(PgConnectionStatus.ACTIVE)
                .build();

        PgConnection saved = pgConnectionRepository.save(entity);
        log.info("Created PG connection: {} ({})", saved.getPgCode(), saved.getId());

        return toDto(saved);
    }

    @Transactional
    public PgConnectionDto update(UUID id, PgConnectionUpdateRequest request) {
        PgConnection entity = getEntityOrThrow(id);

        if (request.getPgName() != null) {
            entity.setPgName(request.getPgName());
        }
        if (request.getApiBaseUrl() != null) {
            entity.setApiBaseUrl(request.getApiBaseUrl());
        }
        if (request.getWebhookBaseUrl() != null) {
            entity.setWebhookBaseUrl(request.getWebhookBaseUrl());
        }
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        Map<String, Object> credentials = entity.getCredentials();
        if (credentials == null) {
            credentials = new HashMap<>();
        }
        if (request.getMerchantId() != null) {
            credentials.put("merchant_id", request.getMerchantId());
        }
        if (request.getApiKey() != null) {
            credentials.put("api_key", request.getApiKey());
        }
        if (request.getSecretKey() != null) {
            credentials.put("secret_key", request.getSecretKey());
        }
        entity.setCredentials(credentials);

        Map<String, Object> config = entity.getConfig();
        if (config == null) {
            config = new HashMap<>();
        }
        if (request.getWebhookSecret() != null) {
            config.put("webhook_secret", request.getWebhookSecret());
        }
        if (request.getTimeoutMs() != null) {
            config.put("timeout_ms", request.getTimeoutMs());
        }
        if (request.getRetryCount() != null) {
            config.put("retry_count", request.getRetryCount());
        }
        entity.setConfig(config.isEmpty() ? null : config);

        PgConnection saved = pgConnectionRepository.save(entity);
        log.info("Updated PG connection: {} ({})", saved.getPgCode(), saved.getId());

        return toDto(saved);
    }

    @Transactional
    public PgConnectionDto updateStatus(UUID id, PgConnectionStatus status) {
        PgConnection entity = getEntityOrThrow(id);
        entity.setStatus(status);

        PgConnection saved = pgConnectionRepository.save(entity);
        log.info("Updated PG connection status: {} -> {}", saved.getPgCode(), status);

        return toDto(saved);
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

    private void validateUniqueConstraints(String pgCode, UUID excludeId) {
        pgConnectionRepository.findByPgCode(pgCode)
                .ifPresent(existing -> {
                    if (excludeId == null || !existing.getId().equals(excludeId)) {
                        throw new ValidationException("이미 존재하는 PG 코드입니다: " + pgCode);
                    }
                });
    }
}
