package com.openlab.qualitos.quality.standards.auditblanc.domain;

import com.openlab.qualitos.quality.standards.ObligationLevel;
import com.openlab.qualitos.quality.standards.RiskLevel;

import java.util.List;
import java.util.Objects;

/**
 * Clause candidate à l'audit blanc, avec son état de preuve côté tenant
 * (Standards Hub §8.4 onglet 7). Value object PUR : aucune dépendance
 * Spring/JPA. Porte le <i>décompte</i> de preuves (jamais leur contenu) :
 * {@code coveredRequirements} sur {@code totalRequirements}.
 *
 * <p>Les règles de priorisation (clauses à risque d'abord) et de criticité
 * (grille ISO/IEC 17021-1) sont portées par ce value object, garantissant un
 * comportement déterministe indépendant de l'IA (l'IA rédige, la règle tranche).
 */
public final class MockAuditClause {

    private final String clauseCode;
    private final String title;
    private final ObligationLevel obligation;
    private final RiskLevel risk;
    private final int totalRequirements;
    private final int coveredRequirements;
    private final List<String> evidenceTypes;

    public MockAuditClause(String clauseCode, String title, ObligationLevel obligation,
                           RiskLevel risk, int totalRequirements, int coveredRequirements,
                           List<String> evidenceTypes) {
        this.clauseCode = requireText(clauseCode, "clauseCode");
        this.title = requireText(title, "title");
        this.obligation = Objects.requireNonNull(obligation, "obligation");
        this.risk = risk == null ? RiskLevel.MEDIUM : risk;
        if (totalRequirements < 0) {
            throw new IllegalArgumentException("totalRequirements must be >= 0");
        }
        if (coveredRequirements < 0 || coveredRequirements > totalRequirements) {
            throw new IllegalArgumentException(
                    "coveredRequirements must be in [0, totalRequirements]");
        }
        this.totalRequirements = totalRequirements;
        this.coveredRequirements = coveredRequirements;
        this.evidenceTypes = evidenceTypes == null ? List.of() : List.copyOf(evidenceTypes);
    }

    public String clauseCode() {
        return clauseCode;
    }

    public String title() {
        return title;
    }

    public ObligationLevel obligation() {
        return obligation;
    }

    public RiskLevel risk() {
        return risk;
    }

    public int totalRequirements() {
        return totalRequirements;
    }

    public int coveredRequirements() {
        return coveredRequirements;
    }

    public List<String> evidenceTypes() {
        return evidenceTypes;
    }

    /** Part d'exigences démontrées par une preuve ∈ [0,1]. */
    public double coverageRatio() {
        return totalRequirements == 0 ? 0d : (double) coveredRequirements / totalRequirements;
    }

    public boolean fullyCovered() {
        return totalRequirements > 0 && coveredRequirements == totalRequirements;
    }

    /**
     * Score de priorité d'audit (élevé = clause à regarder en premier). Combine le
     * caractère obligatoire, la gravité du risque et le <i>défaut</i> de
     * couverture. Une clause obligatoire à risque élevé et non couverte domine la
     * liste.
     *
     * <p>C'est le seul emploi du niveau de risque, et il est fondé : l'ISO 19011,
     * 6.3, demande un programme d'audit établi par les risques. Il dit par où
     * commencer, jamais ce que l'auditeur conclura — cela, c'est
     * {@link #criticality()}, sur la preuve trouvée.
     */
    public double riskScore() {
        double obligationWeight = switch (obligation) {
            case MUST -> 3d;
            case SHOULD -> 1.5d;
            case MAY -> 0.5d;
        };
        double riskWeight = switch (risk) {
            case LOW -> 1d;
            case MEDIUM -> 2d;
            case HIGH -> 3d;
            case CRITICAL -> 4d;
        };
        double gap = 1d - coverageRatio();
        return obligationWeight * riskWeight * (0.25d + 0.75d * gap);
    }

    /**
     * Criticité de l'écart, selon les critères de l'ISO/IEC 17021-1 (3.12, 3.13).
     *
     * <p>La même règle que l'analyse d'écarts, aux mêmes sources : le verbe de la
     * norme dit si le manquement peut être une non-conformité, l'état des preuves
     * de la clause dit lequel des deux degrés s'applique. Aucune preuve dans la
     * clause : la disposition demandée est absente, l'aptitude du système à
     * atteindre ses résultats est en cause — <b>majeure</b>. Des preuves, mais
     * pas sur toutes les exigences : manquement isolé — <b>mineure</b>.
     *
     * <p>Le niveau de risque de la clause n'entre plus ici. Il ordonne le
     * programme d'audit ({@link #riskScore()}), il ne gradue pas un constat :
     * c'est la preuve trouvée, et non une étiquette posée d'avance sur la clause,
     * qui décide de ce qu'un auditeur écrira.
     */
    public MockAuditCriticality criticality() {
        if (fullyCovered() || obligation != ObligationLevel.MUST) {
            return MockAuditCriticality.OBSERVATION;
        }
        return coveredRequirements == 0
                ? MockAuditCriticality.MAJOR
                : MockAuditCriticality.MINOR;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        return value;
    }
}
