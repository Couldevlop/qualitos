package com.openlab.qualitos.quality.retention.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RetentionRuleRepository {

    RetentionRule save(RetentionRule rule);

    Optional<RetentionRule> findById(UUID id);

    List<RetentionRule> findByTenant(UUID tenantId);

    List<RetentionRule> findByTenantAndStatus(UUID tenantId, RetentionRuleStatus status);

    /** Règle ACTIVE pour ce tenant & cette catégorie (au plus une à un instant donné). */
    Optional<RetentionRule> findActiveByCategory(UUID tenantId, String dataCategoryCode);

    void delete(UUID id);
}
