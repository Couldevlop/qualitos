package com.openlab.qualitos.quality.aipmm.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PmmPlanRepository {

    PmmPlan save(PmmPlan plan);

    Optional<PmmPlan> findById(UUID id);

    List<PmmPlan> findByTenant(UUID tenantId);

    List<PmmPlan> findByTenantAndStatus(UUID tenantId, PmmPlanStatus status);

    List<PmmPlan> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId);

    /** Plans actifs dont la prochaine revue est en retard. */
    List<PmmPlan> findOverdueReviews(UUID tenantId, Instant now, int limit);

    Optional<PmmPlan> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
