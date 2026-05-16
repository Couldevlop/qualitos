package com.openlab.qualitos.quality.nis2measures.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Nis2RiskMeasureRepository {

    Nis2RiskMeasure save(Nis2RiskMeasure measure);

    Optional<Nis2RiskMeasure> findById(UUID id);

    List<Nis2RiskMeasure> findByTenant(UUID tenantId);

    List<Nis2RiskMeasure> findByTenantAndStatus(UUID tenantId, Nis2MeasureStatus status);

    List<Nis2RiskMeasure> findByTenantAndCategory(UUID tenantId, Nis2MeasureCategory category);

    Optional<Nis2RiskMeasure> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    /** Mesures non terminales dont la revue est en retard. */
    List<Nis2RiskMeasure> findReviewOverdue(Instant now, int limit);

    void delete(UUID id);
}
