package com.openlab.qualitos.quality.change;

/**
 * Cycle de vie d'une demande de changement (MOC, ADR 0087).
 *
 * <pre>
 * DRAFT ──(envoyer en analyse)──▶ IN_ANALYSIS ──(soumettre, 8 domaines évalués)──▶ IN_VALIDATION
 * IN_VALIDATION ──(tous les rôles requis approuvent)──▶ APPROVED
 * IN_VALIDATION ──(un seul refus, motivé)──▶ REJECTED ──(rouvrir l'analyse)──▶ IN_ANALYSIS
 * </pre>
 *
 * Seul un brouillon se supprime. Les anciens statuts ont été repris par V138 :
 * SUBMITTED et UNDER_REVIEW repartent en analyse (ils n'avaient pas d'analyse
 * d'impact par domaine), IMPLEMENTED devient APPROVED, CANCELLED devient REJECTED.
 */
public enum ChangeRequestStatus {
    DRAFT,
    IN_ANALYSIS,
    IN_VALIDATION,
    APPROVED,
    REJECTED;

    /** Une demande dont le contenu et l'analyse se modifient encore. */
    public boolean open() {
        return this == DRAFT || this == IN_ANALYSIS;
    }
}
