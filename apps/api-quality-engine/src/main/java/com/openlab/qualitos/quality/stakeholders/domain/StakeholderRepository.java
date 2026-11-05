package com.openlab.qualitos.quality.stakeholders.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Le port de persistance du registre.
 *
 * <p>Chaque lecture prend le client en argument : l'isolation est dans la
 * signature, pas dans un appel qu'on pourrait oublier.
 */
public interface StakeholderRepository {

    Stakeholder save(Stakeholder stakeholder);

    Optional<Stakeholder> findByIdAndTenant(UUID id, UUID tenantId);

    List<Stakeholder> findByTenant(UUID tenantId);

    /** Deux fiches de même nom dans la même famille sont un doublon, pas une nuance. */
    boolean nameTaken(UUID tenantId, StakeholderCategory category, String name, UUID exceptId);

    void delete(Stakeholder stakeholder);
}
