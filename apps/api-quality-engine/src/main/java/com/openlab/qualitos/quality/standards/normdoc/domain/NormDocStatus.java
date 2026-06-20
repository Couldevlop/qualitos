package com.openlab.qualitos.quality.standards.normdoc.domain;

/**
 * Cycle de vie d'un document normatif généré (Standards Hub §8.8).
 *
 * <p>Workflow de validation humaine — aucune publication sans signature humaine
 * (CLAUDE.md §18.2 #5, l'IA suggère, l'humain décide) :
 * <pre>
 *   BROUILLON_IA → EN_VALIDATION → APPROUVE
 *   BROUILLON_IA → EN_VALIDATION → REJETE → BROUILLON_IA (reprise possible)
 *   BROUILLON_IA → APPROUVE est INTERDIT (la revue est obligatoire).
 * </pre>
 */
public enum NormDocStatus {
    /** Brouillon généré par l'IA, éditable, non soumis. */
    BROUILLON_IA,
    /** Soumis à la revue humaine. */
    EN_VALIDATION,
    /** Approuvé et signé par un humain (état terminal). */
    APPROUVE,
    /** Rejeté en revue — retourne en brouillon pour reprise. */
    REJETE
}
