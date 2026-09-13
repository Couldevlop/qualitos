package com.openlab.qualitos.quality.apqp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApqpDeliverableEvidenceRepository
        extends JpaRepository<ApqpDeliverableEvidence, UUID> {

    List<ApqpDeliverableEvidence> findByTenantIdAndDeliverableIdOrderByCreatedAtAsc(
            UUID tenantId, UUID deliverableId);

    Optional<ApqpDeliverableEvidence> findByIdAndTenantIdAndDeliverableId(
            UUID id, UUID tenantId, UUID deliverableId);

    long countByTenantIdAndDeliverableId(UUID tenantId, UUID deliverableId);

    /**
     * Poids déjà versé par le client, calculé en base.
     *
     * <p>Le plafond se compte au niveau du CYCLE, qui est l'unité qu'un client
     * possède — il n'en a qu'un. Charger les métadonnées pour additionner un
     * entier serait payer un aller-retour pour rien.
     */
    @Query("select coalesce(sum(e.sizeBytes), 0) from ApqpDeliverableEvidence e"
            + " where e.tenantId = :tenantId")
    long sumSizeBytes(UUID tenantId);

    /**
     * Combien de pièces par livrable, pour tout le cycle d'un client.
     *
     * <p>Une seule requête groupée : la liste d'une phase et la section PPAP
     * affichent « prouvé » sur chaque ligne, et une requête par livrable ferait
     * quarante-huit allers-retours pour autant d'icônes.
     */
    @Query("select e.deliverableId, count(e) from ApqpDeliverableEvidence e"
            + " where e.tenantId = :tenantId group by e.deliverableId")
    List<Object[]> countByDeliverableForTenant(UUID tenantId);

    /**
     * Existence par clé d'objet, sans filtre de client — pour le balayage des
     * orphelins, qui s'exécute hors requête et donc sans contexte. La clé porte
     * déjà le client dans son chemin, et la question posée ici est « quelqu'un
     * revendique-t-il cet objet ? », qui ne dépend d'aucun client.
     */
    boolean existsByObjectKey(String objectKey);
}
