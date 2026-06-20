package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.util.List;
import java.util.Objects;

/**
 * Constat d'écart pour une clause (confrontation question ↔ preuves disponibles
 * du tenant) — Standards Hub §8.4 onglet 7. Value object PUR.
 *
 * <p>La criticité est déterministe (grille du domaine) ; le texte du constat
 * provient de l'IA (ou d'un repli déterministe si l'IA n'a rien rédigé).
 */
public record ClauseGapFinding(
        String clauseCode,
        String title,
        MockAuditCriticality criticality,
        double coverageRatio,
        int totalRequirements,
        int coveredRequirements,
        String finding,
        List<MockAuditQuestion> questions) {

    public ClauseGapFinding {
        if (clauseCode == null || clauseCode.isBlank()) {
            throw new IllegalArgumentException("clauseCode required");
        }
        if (finding == null || finding.isBlank()) {
            throw new IllegalArgumentException("finding required");
        }
        if (coverageRatio < 0d || coverageRatio > 1d) {
            throw new IllegalArgumentException("coverageRatio must be in [0, 1]");
        }
        Objects.requireNonNull(criticality, "criticality");
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
