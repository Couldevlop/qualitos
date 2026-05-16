package com.openlab.qualitos.quality.ratelimit.application;

import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicy;

import java.time.Instant;
import java.util.UUID;

public final class RateLimitDto {

    private RateLimitDto() {}

    public record UpsertPolicyRequest(
            String scope, int windowSeconds, int maxRequests, boolean enabled) {}

    public record PolicyView(
            UUID id, UUID tenantId, String scope,
            int windowSeconds, int maxRequests, boolean enabled,
            Instant createdAt, Instant updatedAt
    ) {
        public static PolicyView of(RateLimitPolicy p) {
            return new PolicyView(p.getId(), p.getTenantId(), p.getScope(),
                    p.getWindowSeconds(), p.getMaxRequests(), p.isEnabled(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
