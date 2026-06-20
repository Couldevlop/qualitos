package com.openlab.qualitos.quality.standards.normdoc.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance pour les documents normatifs générés (clean architecture :
 * interface dans le domaine, adapter JPA dans l'infrastructure). Le filtrage par
 * tenant est assuré par l'adapter (tenant issu du JWT).
 */
public interface NormDocRepository {

    NormativeDocument save(NormativeDocument doc);

    Optional<NormativeDocument> findById(UUID id);

    List<NormativeDocument> findByTenant(UUID tenantId);

    List<NormativeDocument> findByTenantAndStatus(UUID tenantId, NormDocStatus status);

    void delete(UUID id);
}
