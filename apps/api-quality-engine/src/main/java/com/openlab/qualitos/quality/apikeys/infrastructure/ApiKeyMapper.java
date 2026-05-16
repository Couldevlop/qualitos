package com.openlab.qualitos.quality.apikeys.infrastructure;

import com.openlab.qualitos.quality.apikeys.domain.ApiKey;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

final class ApiKeyMapper {

    private ApiKeyMapper() {}

    static ApiKey toDomain(ApiKeyJpaEntity e) {
        Set<String> scopes = e.getScopesCsv() == null || e.getScopesCsv().isBlank()
                ? Set.of()
                : new TreeSet<>(Arrays.asList(e.getScopesCsv().split(",")));
        return new ApiKey(
                e.getId(), e.getTenantId(), e.getName(), e.getPrefix(),
                e.getHashedSecret(), scopes,
                e.getStatus(), e.getCreatedAt(), e.getCreatedBy(),
                e.getExpiresAt(), e.getLastUsedAt(),
                e.getRevokedAt(), e.getRevokedBy());
    }

    static ApiKeyJpaEntity toEntity(ApiKey k, ApiKeyJpaEntity existing) {
        ApiKeyJpaEntity e = existing != null ? existing : new ApiKeyJpaEntity();
        if (k.getId() != null) e.setId(k.getId());
        e.setTenantId(k.getTenantId());
        e.setName(k.getName());
        e.setPrefix(k.getPrefix());
        e.setHashedSecret(k.getHashedSecret());
        e.setScopesCsv(k.getScopes().isEmpty() ? null : String.join(",", k.getScopes()));
        e.setStatus(k.getStatus());
        e.setCreatedAt(k.getCreatedAt());
        e.setCreatedBy(k.getCreatedBy());
        e.setExpiresAt(k.getExpiresAt());
        e.setLastUsedAt(k.getLastUsedAt());
        e.setRevokedAt(k.getRevokedAt());
        e.setRevokedBy(k.getRevokedBy());
        return e;
    }
}
