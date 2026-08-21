package com.openlab.qualitos.quality.nonconformity;

import java.time.Instant;
import java.util.UUID;

/**
 * Une non-conformité vient d'être enregistrée.
 *
 * <p>Publié pour TOUTE création, même sans produit : le filtre appartient au
 * consommateur. Un émetteur qui présumerait de ce qui intéresse la suite
 * empêcherait d'y brancher quoi que ce soit d'autre sans le rouvrir.
 *
 * <p>L'événement porte son tenant, comme {@code CapaTransitionEvent} : son
 * consommateur s'exécute après le commit, hors du contexte d'exécution qui l'a
 * produit, et un fait d'audit qui dépend de l'état ambiant du fil est fragile.
 */
public record NcCreatedEvent(
        UUID tenantId,
        UUID ncId,
        UUID productId,
        UUID fmeaItemId,
        String title,
        String description,
        Instant detectedAt) {
}
