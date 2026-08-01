package com.openlab.qualitos.quality.ratelimit.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RateLimitPolicyRepository {

    RateLimitPolicy save(RateLimitPolicy policy);

    Optional<RateLimitPolicy> findById(UUID id);

    /** Politique active pour ce (tenant, scope) ou empty si rien configuré. */
    Optional<RateLimitPolicy> findEnabled(UUID tenantId, String scope);

    /**
     * Politique du périmètre, ACTIVÉE OU NON.
     *
     * <p>Distincte de {@link #findEnabled} : l'application d'un quota ne s'intéresse
     * qu'aux politiques actives, mais la mise à jour doit retrouver une politique
     * suspendue — sinon elle tente une insertion et bute sur la contrainte d'unicité
     * (tenant_id, scope), ce qui rendait toute suspension irréversible.
     */
    Optional<RateLimitPolicy> findAnyByScope(UUID tenantId, String scope);

    List<RateLimitPolicy> findAllByTenantId(UUID tenantId);

    void delete(RateLimitPolicy policy);
}
