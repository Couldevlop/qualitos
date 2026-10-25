package com.openlab.qualitos.quality.airouting.domain;

import java.util.Optional;
import java.util.UUID;

/** Port de persistance de la politique IA d'un tenant (ADR 0102). */
public interface TenantAiPolicyRepository {

    Optional<TenantAiPolicy> findByTenantId(UUID tenantId);

    /** Insère ou remplace : un tenant n'a qu'une politique. */
    TenantAiPolicy save(TenantAiPolicy policy);

    /** Retire la politique ; le tenant revient au défaut de la plateforme. */
    void deleteByTenantId(UUID tenantId);
}
