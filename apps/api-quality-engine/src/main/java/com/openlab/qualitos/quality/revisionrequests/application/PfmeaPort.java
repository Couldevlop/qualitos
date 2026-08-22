package com.openlab.qualitos.quality.revisionrequests.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Ce que le moteur de propositions demande au PFMEA : lire une cotation, et —
 * une fois la proposition acceptée — la corriger.
 *
 * <p>Un port plutôt que les dépôts Spring Data du module {@code risk} : le moteur
 * dicte ce dont il a besoin, l'infrastructure s'y plie, et un test de règle métier
 * n'a pas à monter un contexte JPA. Chaque méthode prend son tenant : l'écouteur
 * travaille après commit, hors de la requête qui l'a produit.
 */
public interface PfmeaPort {

    /** La ligne d'analyse visée, si elle existe dans ce tenant. */
    Optional<PfmeaItemSnapshot> item(UUID tenantId, UUID fmeaItemId);

    /** Le projet PFMEA en vigueur du produit, s'il y en a un. */
    Optional<UUID> activeProjectOf(UUID tenantId, UUID productId);

    /** Vrai si le projet est en vigueur — donc figé jusqu'à l'ouverture d'une révision. */
    boolean isProjectActive(UUID tenantId, UUID projectId);

    /** Rouvre le projet en brouillon et incrémente sa révision. */
    void openRevision(UUID tenantId, UUID projectId);

    /** Corrige une cote (« occurrence » ou « detection ») ; le RPN et l'AP se recalculent. */
    void updateRating(UUID tenantId, UUID fmeaItemId, String field, int value);

    /** Ajoute une ligne d'analyse au projet et rend son identifiant. */
    UUID addItem(UUID tenantId, UUID projectId, String failureMode, String failureEffect);

    /**
     * Instantané d'une ligne d'analyse : ce que le moteur lit, jamais l'entité JPA
     * elle-même, qui traînerait derrière elle son contexte de persistance.
     */
    record PfmeaItemSnapshot(UUID id, UUID projectId, UUID productId, String failureMode,
                             int severity, int occurrence, int detection) {}
}
