package com.openlab.qualitos.quality.aiincidents.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiIncidentRepository {

    AiIncident save(AiIncident incident);

    Optional<AiIncident> findById(UUID id);

    List<AiIncident> findByTenant(UUID tenantId);

    List<AiIncident> findByTenantAndStatus(UUID tenantId, AiIncidentStatus status);

    List<AiIncident> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId);

    List<AiIncident> findByTenantAndSeverity(UUID tenantId, AiIncidentSeverity severity);

    /** Incidents non notifiés au régulateur dont l'échéance est dépassée. */
    List<AiIncident> findOverdueForRegulatorNotification(UUID tenantId, Instant now, int limit);

    Optional<AiIncident> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
