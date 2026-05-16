package com.openlab.qualitos.quality.cyberincidents.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CyberIncidentRepository {

    CyberIncident save(CyberIncident incident);

    Optional<CyberIncident> findById(UUID id);

    List<CyberIncident> findByTenant(UUID tenantId);

    List<CyberIncident> findByTenantAndStatus(UUID tenantId, CyberIncidentStatus status);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    Optional<CyberIncident> findByTenantAndReference(UUID tenantId, String reference);

    /** Non-terminaux dont la deadline 24h est dépassée et alerte non envoyée. */
    List<CyberIncident> findEarlyWarningOverdue(Instant now, int limit);

    /** Non-terminaux dont la deadline 72h est dépassée et évaluation non envoyée. */
    List<CyberIncident> findInitialAssessmentOverdue(Instant now, int limit);

    /** Non-terminaux dont la deadline 30j est dépassée et rapport final non envoyé. */
    List<CyberIncident> findFinalReportOverdue(Instant now, int limit);

    void delete(UUID id);
}
