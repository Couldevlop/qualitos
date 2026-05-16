package com.openlab.qualitos.quality.apikeys.application;

import com.openlab.qualitos.quality.apikeys.domain.ApiKey;
import com.openlab.qualitos.quality.apikeys.domain.ApiKeyStatus;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class ApiKeyDto {

    private ApiKeyDto() {}

    public record CreateRequest(String name, Set<String> scopes, Instant expiresAt, UUID actor) {}
    public record RotateRequest(UUID actor) {}
    public record RevokeRequest(UUID actor) {}

    /** Vue qui N'expose PAS le hash. Le secret en clair n'apparaît que dans IssuedKey. */
    public record View(
            UUID id, UUID tenantId, String name, String prefix,
            List<String> scopes, ApiKeyStatus status,
            Instant createdAt, UUID createdBy,
            Instant expiresAt, Instant lastUsedAt,
            Instant revokedAt, UUID revokedBy
    ) {
        public static View of(ApiKey k) {
            return new View(
                    k.getId(), k.getTenantId(), k.getName(), k.getPrefix(),
                    new TreeSet<>(k.getScopes()).stream().toList(),
                    k.getStatus(),
                    k.getCreatedAt(), k.getCreatedBy(),
                    k.getExpiresAt(), k.getLastUsedAt(),
                    k.getRevokedAt(), k.getRevokedBy());
        }
    }

    /**
     * Renvoyée UNE SEULE FOIS à la création/rotation. {@code plaintext} ne sera
     * jamais re-disponible — le client doit le copier immédiatement.
     */
    public record IssuedKey(View view, String plaintext) {}
}
