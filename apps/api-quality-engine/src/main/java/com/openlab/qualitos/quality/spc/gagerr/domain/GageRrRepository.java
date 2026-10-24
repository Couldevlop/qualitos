package com.openlab.qualitos.quality.spc.gagerr.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Le port de persistance des études R&amp;R. Chaque lecture prend le tenant en
 * argument : l'isolation est dans la signature, pas dans la discipline.
 */
public interface GageRrRepository {

    GageRrStudy save(GageRrStudy study);

    Optional<GageRrStudy> findByIdAndTenant(UUID id, UUID tenantId);

    List<GageRrStudy> findByTenant(UUID tenantId);

    void delete(GageRrStudy study);
}
