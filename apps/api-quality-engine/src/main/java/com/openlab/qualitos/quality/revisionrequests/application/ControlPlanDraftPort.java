package com.openlab.qualitos.quality.revisionrequests.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Le brouillon de control plan dans lequel une proposition acceptée vient atterrir.
 *
 * <p>Le port cache l'ouverture de révision : le moteur demande « où puis-je écrire
 * pour ce produit ? » et l'infrastructure, elle, sait qu'il faut parfois ouvrir une
 * révision du plan en vigueur et y recopier ses lignes.
 */
public interface ControlPlanDraftPort {

    /**
     * Le plan modifiable du produit : le brouillon existant, sinon une révision
     * fraîchement ouverte du plan en vigueur. Vide si le produit n'a aucun plan.
     */
    Optional<UUID> draftPlanFor(UUID tenantId, UUID productId);

    UUID addLine(UUID tenantId, UUID planId, String characteristicLabel,
                 String controlMethod, UUID fmeaItemId);
}
