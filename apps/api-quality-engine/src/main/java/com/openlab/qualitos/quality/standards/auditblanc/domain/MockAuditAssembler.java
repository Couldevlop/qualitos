package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assemble le rapport d'écarts (gap analysis) à partir des clauses à risque et
 * de la sortie IA (questions + constats) — Standards Hub §8.4 onglet 7.
 * Domaine PUR : déterministe, testable sans IA.
 *
 * <p>Règle d'or : la criticité de chaque écart est calculée par le domaine
 * ({@link MockAuditClause#criticality()}), JAMAIS par un libellé libre du LLM.
 * Le constat textuel provient de l'IA si présent, sinon d'un repli déterministe
 * dérivé du seul état de preuve (aucun trou silencieux dans le rapport).
 * Les clauses sont triées par priorité de risque décroissante.
 */
public final class MockAuditAssembler {

    private MockAuditAssembler() {
    }

    public static List<ClauseGapFinding> assemble(
            List<MockAuditClause> clauses,
            List<MockAuditQuestion> questions,
            Map<String, String> aiFindings) {

        Map<String, List<MockAuditQuestion>> byClause = new LinkedHashMap<>();
        for (MockAuditQuestion q : questions) {
            byClause.computeIfAbsent(q.clauseCode(), k -> new ArrayList<>()).add(q);
        }

        List<MockAuditClause> ordered = new ArrayList<>(clauses);
        ordered.sort(Comparator.comparingDouble(MockAuditClause::riskScore).reversed());

        List<ClauseGapFinding> gaps = new ArrayList<>(ordered.size());
        for (MockAuditClause c : ordered) {
            String aiFinding = aiFindings.get(c.clauseCode());
            String finding = (aiFinding != null && !aiFinding.isBlank())
                    ? aiFinding
                    : deterministicFinding(c);
            gaps.add(new ClauseGapFinding(
                    c.clauseCode(), c.title(), c.criticality(),
                    c.coverageRatio(), c.totalRequirements(), c.coveredRequirements(),
                    finding, byClause.getOrDefault(c.clauseCode(), List.of())));
        }
        return gaps;
    }

    /** Constat de repli, dérivé du seul état de preuve (jamais inventé). */
    private static String deterministicFinding(MockAuditClause c) {
        if (c.fullyCovered()) {
            return "Clause " + c.clauseCode() + " couverte par des preuves ("
                    + c.coveredRequirements() + "/" + c.totalRequirements()
                    + ") — vérifier la fraîcheur et la pertinence des preuves liées.";
        }
        if (c.coveredRequirements() == 0) {
            return "Aucune preuve liée à la clause " + c.clauseCode()
                    + " (" + c.title() + "). Exigences non démontrées : "
                    + c.totalRequirements() + ".";
        }
        return "Couverture partielle de la clause " + c.clauseCode() + " : "
                + c.coveredRequirements() + "/" + c.totalRequirements()
                + " exigences démontrées par une preuve.";
    }

    /** Décompte (major, minor, observation) sur l'ensemble des écarts. */
    public static int[] countByCriticality(List<ClauseGapFinding> gaps) {
        int major = 0;
        int minor = 0;
        int observation = 0;
        for (ClauseGapFinding g : gaps) {
            switch (g.criticality()) {
                case MAJOR -> major++;
                case MINOR -> minor++;
                default -> observation++;
            }
        }
        return new int[] {major, minor, observation};
    }
}
