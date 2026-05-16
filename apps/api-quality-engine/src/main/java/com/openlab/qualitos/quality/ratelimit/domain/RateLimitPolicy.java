package com.openlab.qualitos.quality.ratelimit.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Politique de rate-limit pour un (tenant, scope). Implémente un compteur à
 * fenêtre fixe : {@code maxRequests} requêtes sur {@code windowSeconds} secondes.
 *
 * Sécurité (OWASP A04 Insecure Design / A07 Auth Failures) :
 *  - {@code maxRequests} ≥ 1 et borné en haut pour limiter le risque d'abuser
 *    du système d'admin lui-même (un admin malveillant pourrait sinon désactiver
 *    le rate limit en mettant Long.MAX).
 *  - Le scope est régex-restreint ; impossible d'injecter de caractères spéciaux
 *    dans le compteur en mémoire.
 */
public final class RateLimitPolicy {

    public static final int MAX_WINDOW_SECONDS = 86400;       // 24h
    public static final int MAX_REQUESTS_PER_WINDOW = 1_000_000;

    static final Pattern SCOPE_FORMAT = Pattern.compile("^[a-z][a-z0-9._:-]{0,99}$");

    private UUID id;
    private final UUID tenantId;
    private final String scope;
    private int windowSeconds;
    private int maxRequests;
    private boolean enabled;
    private final Instant createdAt;
    private Instant updatedAt;

    public RateLimitPolicy(UUID id, UUID tenantId, String scope,
                           int windowSeconds, int maxRequests, boolean enabled,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.scope = requireScope(scope);
        this.windowSeconds = requireWindow(windowSeconds);
        this.maxRequests = requireMax(maxRequests);
        this.enabled = enabled;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static RateLimitPolicy create(UUID tenantId, String scope,
                                         int windowSeconds, int maxRequests, Instant now) {
        return new RateLimitPolicy(null, tenantId, scope,
                windowSeconds, maxRequests, true, now, now);
    }

    public void update(int newWindowSeconds, int newMaxRequests, boolean enabled, Instant now) {
        this.windowSeconds = requireWindow(newWindowSeconds);
        this.maxRequests = requireMax(newMaxRequests);
        this.enabled = enabled;
        this.updatedAt = now;
    }

    public void assignId(UUID id) { this.id = id; }

    private static String requireScope(String s) {
        if (s == null || !SCOPE_FORMAT.matcher(s).matches()) {
            throw new RateLimitPolicyException("invalid scope: " + s);
        }
        return s;
    }
    private static int requireWindow(int w) {
        if (w < 1 || w > MAX_WINDOW_SECONDS) {
            throw new RateLimitPolicyException(
                    "windowSeconds out of bounds [1.." + MAX_WINDOW_SECONDS + "]: " + w);
        }
        return w;
    }
    private static int requireMax(int m) {
        if (m < 1 || m > MAX_REQUESTS_PER_WINDOW) {
            throw new RateLimitPolicyException(
                    "maxRequests out of bounds [1.." + MAX_REQUESTS_PER_WINDOW + "]: " + m);
        }
        return m;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getScope() { return scope; }
    public int getWindowSeconds() { return windowSeconds; }
    public int getMaxRequests() { return maxRequests; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
