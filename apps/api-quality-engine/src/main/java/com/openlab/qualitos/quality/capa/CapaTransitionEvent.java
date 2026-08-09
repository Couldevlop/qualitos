package com.openlab.qualitos.quality.capa;

import java.util.Map;
import java.util.UUID;

/**
 * Une transition de dossier CAPA, prête à être annoncée aux systèmes abonnés.
 *
 * <p>Événement applicatif interne, publié dans la transaction et consommé APRÈS
 * sa validation. Il porte son propre tenant plutôt que de compter sur le contexte
 * d'exécution : le consommateur s'exécute plus tard, et un fait d'audit qui
 * dépend de l'état ambiant du fil d'exécution est un fait fragile.
 *
 * @param tenantId   tenant du dossier, issu de l'entité et donc du jeton
 * @param transition ce qui vient d'arriver au dossier
 * @param payload    état d'arrivée du dossier, déjà réduit à ce qui se publie
 */
public record CapaTransitionEvent(UUID tenantId, CapaTransition transition, Map<String, Object> payload) {
}
