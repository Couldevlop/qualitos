package com.openlab.qualitos.quality.dpia.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DpiaRepository {

    Dpia save(Dpia dpia);

    Optional<Dpia> findById(UUID id);

    List<Dpia> findByTenant(UUID tenantId);

    List<Dpia> findByTenantAndStatus(UUID tenantId, DpiaStatus status);

    Optional<Dpia> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
