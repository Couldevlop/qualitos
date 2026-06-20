package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance des exécutions d'audit blanc (Standards Hub §8.4 onglet 7).
 * L'adapter filtre TOUJOURS par tenant (issu du JWT) et refuse les écritures /
 * lectures cross-tenant (OWASP A01).
 */
public interface MockAuditRunRepository {

    MockAuditRun save(MockAuditRun run);

    Optional<MockAuditRun> findById(UUID id);

    /** Exécutions du tenant pour une adoption donnée, la plus récente d'abord. */
    List<MockAuditRun> findByAdoption(UUID adoptionId);
}
