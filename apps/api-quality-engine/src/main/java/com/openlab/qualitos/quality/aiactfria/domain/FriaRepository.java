package com.openlab.qualitos.quality.aiactfria.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriaRepository {

    Fria save(Fria fria);

    Optional<Fria> findById(UUID id);

    List<Fria> findByTenant(UUID tenantId);

    List<Fria> findByTenantAndStatus(UUID tenantId, FriaStatus status);

    List<Fria> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId);

    Optional<Fria> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
