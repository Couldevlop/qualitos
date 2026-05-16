package com.openlab.qualitos.quality.tenantmodules.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleActivationRepository {

    ModuleActivation save(ModuleActivation activation);

    Optional<ModuleActivation> findById(UUID id);

    /** Activation non-terminale (TRIAL/ACTIVE/SUSPENDED) — au plus une par (tenant, code). */
    Optional<ModuleActivation> findOpenByTenantIdAndCode(UUID tenantId, String code);

    /** Toutes les activations (historique inclus). */
    List<ModuleActivation> findAllByTenantId(UUID tenantId);

    /** Activations actuellement enabled (TRIAL ou ACTIVE). */
    List<ModuleActivation> findEnabledByTenantId(UUID tenantId);

    /** Activations dont la date d'expiration / trial est passée et statut non terminal. */
    List<ModuleActivation> findDueForExpiration(java.time.Instant now, int limit);
}
