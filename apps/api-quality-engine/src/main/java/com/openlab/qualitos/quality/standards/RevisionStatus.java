package com.openlab.qualitos.quality.standards;

/** Statut d'une révision normative dans la veille (Standards Hub §8.4 onglet 8, §8.10). */
public enum RevisionStatus {
    /** Version en vigueur. */
    CURRENT,
    /** Révision planifiée / à venir. */
    PLANNED,
    /** Version remplacée. */
    SUPERSEDED
}
