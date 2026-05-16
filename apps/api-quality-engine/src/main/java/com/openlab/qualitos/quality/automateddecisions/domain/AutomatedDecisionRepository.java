package com.openlab.qualitos.quality.automateddecisions.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AutomatedDecisionRepository {

    AutomatedDecisionRecord save(AutomatedDecisionRecord record);

    Optional<AutomatedDecisionRecord> findById(UUID id);

    List<AutomatedDecisionRecord> findByTenant(UUID tenantId);

    List<AutomatedDecisionRecord> findByTenantAndStatus(UUID tenantId, AutomatedDecisionStatus status);

    Optional<AutomatedDecisionRecord> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
