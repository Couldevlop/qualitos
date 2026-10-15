package com.openlab.qualitos.quality.change.domain;

/**
 * Ce qui s'inscrit à l'historique d'une demande. L'écran traduit le code ;
 * le détail (rôle, commentaire) est recopié tel qu'il était au moment des faits.
 */
public enum MocHistoryEvent {
    CREATED,
    UPDATED,
    SENT_TO_ANALYSIS,
    ANALYSIS_COMPLETED,
    SUBMITTED,
    VALIDATION_APPROVED,
    VALIDATION_REJECTED,
    APPROVED,
    REJECTED,
    REOPENED,
    /** Rappel envoyé aux titulaires d'un rôle qui n'a pas statué dans le délai. */
    REMINDER_SENT,
    /** Reprise V138 : l'ancien statut d'une demande antérieure au MOC. */
    LEGACY_STATUS,
    /** Reprise V138 : une décision de l'ancien circuit d'approbation par personne. */
    LEGACY_DECISION,
    /** Reprise V138 : un objet lié par l'ancienne analyse d'impact. */
    LEGACY_IMPACT
}
