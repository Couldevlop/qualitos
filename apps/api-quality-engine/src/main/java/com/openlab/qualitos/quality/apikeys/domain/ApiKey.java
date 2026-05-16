package com.openlab.qualitos.quality.apikeys.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Agrégat clé d'API (OWASP A01 broken access / A02 crypto / A07 auth).
 *
 * Sécurité :
 *  - Le secret en clair n'est JAMAIS persisté. Seul le bcrypt hash est stocké.
 *  - Le {@link #prefix} (8 chars) sert d'index pour résoudre une clé reçue sans
 *    révéler le secret. Il fait partie de la clé en clair (qos_PREFIX_…).
 *  - {@link #lastUsedAt} mis à jour à chaque validation réussie ; toute autre
 *    modification après création passe par {@link #rotateSecret} ou {@link #revoke}.
 *  - Le bcrypt utilisé côté adapter doit avoir strength ≥ 12 (≈ 250 ms / hash).
 *  - Comparaison via {@link ApiKeyHasher#matches(String, String)} qui DOIT utiliser
 *    une comparaison à temps constant (bcrypt le fait nativement).
 */
public final class ApiKey {

    private UUID id;
    private final UUID tenantId;
    private final String name;
    private final String prefix;          // public, indexable
    private String hashedSecret;          // bcrypt
    private final Set<String> scopes;     // ordered for canonical equality
    private ApiKeyStatus status;
    private final Instant createdAt;
    private final UUID createdBy;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant revokedAt;
    private UUID revokedBy;

    public ApiKey(UUID id, UUID tenantId, String name, String prefix,
                  String hashedSecret, Set<String> scopes,
                  ApiKeyStatus status, Instant createdAt, UUID createdBy,
                  Instant expiresAt, Instant lastUsedAt,
                  Instant revokedAt, UUID revokedBy) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.name = require(name, "name");
        this.prefix = require(prefix, "prefix");
        this.hashedSecret = require(hashedSecret, "hashedSecret");
        this.scopes = new TreeSet<>(scopes == null ? Set.of() : scopes);
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.revokedAt = revokedAt;
        this.revokedBy = revokedBy;
    }

    /** Factory : nouvelle clé ACTIVE. */
    public static ApiKey issued(UUID tenantId, String name, String prefix,
                                String hashedSecret, Set<String> scopes,
                                Instant expiresAt, UUID actor, Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new ApiKeyStateException("expiresAt must be in the future");
        }
        return new ApiKey(null, tenantId, name, prefix, hashedSecret, scopes,
                ApiKeyStatus.ACTIVE, now, actor, expiresAt, null, null, null);
    }

    public void rotateSecret(String newHashedSecret, Instant now) {
        ensureActive();
        this.hashedSecret = require(newHashedSecret, "hashedSecret");
        this.lastUsedAt = null; // invalidation côté usage
    }

    public void revoke(UUID actor, Instant now) {
        if (status != ApiKeyStatus.ACTIVE) {
            throw new ApiKeyStateException("Only ACTIVE keys can be revoked (current: " + status + ")");
        }
        this.status = ApiKeyStatus.REVOKED;
        this.revokedAt = now;
        this.revokedBy = actor;
    }

    /** Marque la clé EXPIRED si {@code expiresAt} dépassé. Idempotent. */
    public boolean expireIfDue(Instant now) {
        if (status != ApiKeyStatus.ACTIVE) return false;
        if (expiresAt != null && !now.isBefore(expiresAt)) {
            status = ApiKeyStatus.EXPIRED;
            return true;
        }
        return false;
    }

    public void recordUsage(Instant now) {
        if (status != ApiKeyStatus.ACTIVE) {
            throw new ApiKeyStateException("Cannot use a non-ACTIVE key");
        }
        if (expiresAt != null && !now.isBefore(expiresAt)) {
            status = ApiKeyStatus.EXPIRED;
            throw new ApiKeyStateException("Key expired");
        }
        this.lastUsedAt = now;
    }

    public boolean hasScope(String scope) { return scopes.contains(scope); }

    public boolean isUsable() { return status == ApiKeyStatus.ACTIVE; }

    private void ensureActive() {
        if (status != ApiKeyStatus.ACTIVE)
            throw new ApiKeyStateException("Key not ACTIVE: " + status);
    }

    private static String require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        return v;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getPrefix() { return prefix; }
    public String getHashedSecret() { return hashedSecret; }
    public Set<String> getScopes() { return java.util.Collections.unmodifiableSet(scopes); }
    public ApiKeyStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public UUID getRevokedBy() { return revokedBy; }
}
