package com.openlab.qualitos.quality.ratelimit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RateLimitPolicyRepository {

    RateLimitPolicy save(RateLimitPolicy policy);

    Optional<RateLimitPolicy> findById(UUID id);

    /** Politique active pour ce (tenant, scope) ou empty si rien configuré. */
    Optional<RateLimitPolicy> findEnabled(UUID tenantId, String scope);

    List<RateLimitPolicy> findAllByTenantId(UUID tenantId);

    void delete(RateLimitPolicy policy);
}
