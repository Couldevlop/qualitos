package com.openlab.qualitos.quality.aiact.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiSystemRepository {

    AiSystem save(AiSystem system);

    Optional<AiSystem> findById(UUID id);

    List<AiSystem> findByTenant(UUID tenantId);

    List<AiSystem> findByTenantAndStatus(UUID tenantId, AiSystemStatus status);

    List<AiSystem> findByTenantAndRiskClassification(UUID tenantId, AiRiskClassification riskClassification);

    Optional<AiSystem> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
