package com.openlab.qualitos.quality.automateddecisions.domain;

/**
 * <ul>
 *   <li>DRAFT — éditable, peut être activé ou supprimé.</li>
 *   <li>ACTIVE — décision en production. Immutable hors champs opérationnels.</li>
 *   <li>DEPRECATED — encore en production mais signalé pour remplacement.</li>
 *   <li>ARCHIVED — terminal historique.</li>
 * </ul>
 */
public enum AutomatedDecisionStatus {
    DRAFT,
    ACTIVE,
    DEPRECATED,
    ARCHIVED
}
