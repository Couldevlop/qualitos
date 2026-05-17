package com.openlab.qualitos.quality.aiqms.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiQmsRepository {

    AiQms save(AiQms qms);

    Optional<AiQms> findById(UUID id);

    List<AiQms> findByTenant(UUID tenantId);

    List<AiQms> findByTenantAndStatus(UUID tenantId, AiQmsStatus status);

    Optional<AiQms> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReferenceAndVersion(UUID tenantId, String reference, String version);

    void delete(UUID id);
}
