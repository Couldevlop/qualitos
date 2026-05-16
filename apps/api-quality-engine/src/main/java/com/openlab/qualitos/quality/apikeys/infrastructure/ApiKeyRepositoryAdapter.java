package com.openlab.qualitos.quality.apikeys.infrastructure;

import com.openlab.qualitos.quality.apikeys.domain.ApiKey;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ApiKeyRepositoryAdapter implements ApiKeyRepository {

    private final ApiKeyJpaRepository jpa;

    public ApiKeyRepositoryAdapter(ApiKeyJpaRepository jpa) { this.jpa = jpa; }

    @Override
    @Transactional
    public ApiKey save(ApiKey key) {
        ApiKeyJpaEntity existing = key.getId() != null
                ? jpa.findById(key.getId()).orElse(null) : null;
        ApiKeyJpaEntity saved = jpa.save(ApiKeyMapper.toEntity(key, existing));
        if (key.getId() == null) key.assignId(saved.getId());
        return ApiKeyMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiKey> findById(UUID id) {
        return jpa.findById(id).map(ApiKeyMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApiKey> findByPrefix(String prefix) {
        return jpa.findByPrefix(prefix).map(ApiKeyMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> findAllByTenantId(UUID tenantId) {
        return jpa.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(ApiKeyMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKey> findExpirable(Instant now, int limit) {
        return jpa.findExpirable(now, PageRequest.of(0, limit))
                .stream().map(ApiKeyMapper::toDomain).toList();
    }
}
