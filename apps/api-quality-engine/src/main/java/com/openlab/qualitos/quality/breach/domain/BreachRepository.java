package com.openlab.qualitos.quality.breach.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BreachRepository {

    BreachIncident save(BreachIncident incident);

    Optional<BreachIncident> findById(UUID id);

    List<BreachIncident> findByTenant(UUID tenantId);

    List<BreachIncident> findByTenantAndStatus(UUID tenantId, BreachStatus status);

    /** Non-terminaux dont la deadline DPA (72h) est dépassée et non encore notifiés. */
    List<BreachIncident> findDpaOverdue(Instant now, int limit);

    boolean existsByTenantAndReference(UUID tenantId, String internalReference);
}
