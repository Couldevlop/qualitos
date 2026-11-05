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

    /**
     * Les fiches dont une exigence est rattachée à ce dossier CAPA.
     *
     * <p>Sert la boucle de retour de clôture : on ne sait pas, depuis le module
     * CAPA, quelle partie intéressée a ouvert le dossier, et balayer tout le
     * registre pour le retrouver serait un parcours complet à chaque clôture.
     */
    List<Stakeholder> findByCapa(UUID tenantId, UUID capaId);

    void delete(Stakeholder stakeholder);
}
