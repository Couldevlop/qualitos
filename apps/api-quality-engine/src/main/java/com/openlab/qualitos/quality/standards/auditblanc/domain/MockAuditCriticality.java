package com.openlab.qualitos.quality.standards.auditblanc.domain;

/**
 * Criticité d'un écart de clause restitué par l'audit blanc IA avancé
 * (Standards Hub §8.4 onglet 7). Même grille que la certification à blanc, aux
 * définitions de l'ISO/IEC 17021-1 (3.12, 3.13) : une clause obligatoire dont
 * <i>aucune</i> exigence n'est démontrée est une NC <b>majeure</b> — la
 * disposition est absente, l'aptitude du système est en cause ; une clause
 * obligatoire démontrée en partie, une NC <b>mineure</b> ; le reste (SHOULD/MAY,
 * ou clause couverte), une <b>observation</b>, qui n'est pas une
 * non-conformité.
 *
 * <p>Domaine PUR : aucune dépendance Spring/JPA.
 */
public enum MockAuditCriticality {
    MAJOR(0, "high"),
    MINOR(1, "medium"),
    OBSERVATION(2, "low");

    private final int rank;
    private final String remediationPriority;

    MockAuditCriticality(int rank, String remediationPriority) {
        this.rank = rank;
        this.remediationPriority = remediationPriority;
    }

    /** Ordre de présentation (0 = majeur d'abord). */
    public int rank() {
        return rank;
    }

    /** Priorité de remédiation associée (alignée sur la criticité CAPA). */
    public String remediationPriority() {
        return remediationPriority;
    }
}
