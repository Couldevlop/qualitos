package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit un plan de remédiation actionnable à partir des écarts de l'audit
 * blanc (Standards Hub §8.4 onglet 7). Domaine PUR (déterministe, testable
 * sans IA) : à chaque écart MAJEUR ou MINEUR correspond une action, orientée
 * vers le module QualitOS pertinent selon les types de preuve attendus
 * (référentiel transverse §3.6). Les observations ne génèrent pas d'action
 * (pistes d'amélioration, pas de non-conformité).
 *
 * <p>Le plan est ordonné par criticité décroissante (les écarts majeurs d'abord).
 */
public final class RemediationPlanner {

    private RemediationPlanner() {
    }

    public static List<RemediationAction> plan(List<ClauseGapFinding> gaps) {
        List<ClauseGapFinding> actionable = new ArrayList<>();
        for (ClauseGapFinding g : gaps) {
            if (g.criticality() != MockAuditCriticality.OBSERVATION) {
                actionable.add(g);
            }
        }
        actionable.sort((a, b) -> a.criticality().rank() - b.criticality().rank());

        List<RemediationAction> actions = new ArrayList<>(actionable.size());
        for (ClauseGapFinding g : actionable) {
            actions.add(new RemediationAction(
                    g.clauseCode(),
                    g.criticality(),
                    g.criticality().remediationPriority(),
                    targetModule(g),
                    describeAction(g)));
        }
        return actions;
    }

    /**
     * Oriente vers le module QualitOS le plus pertinent. Une couverture nulle
     * appelle d'abord la production de la preuve (document / formation / audit) ;
     * une couverture partielle appelle un pilotage (cycle PDCA) pour finaliser.
     */
    /** Mots-clés (titre normalisé) orientant la remédiation vers un module. */
    private static final List<String> TRAINING_HINTS = List.of(
            " formation", "compétence", "competence", "sensibilis", "habilitation");
    private static final List<String> AUDIT_HINTS = List.of(
            "audit", "surveillance", "contrôle", "controle");

    private static String targetModule(ClauseGapFinding g) {
        if (g.coveredRequirements() > 0 && g.coverageRatio() < 1d) {
            return "PDCA"; // finaliser un dispositif déjà entamé
        }
        // « formation » précédé d'un espace (ou en tête) : évite le faux positif « information ».
        String hint = " " + lower(g.title());
        if (containsAny(hint, TRAINING_HINTS)) {
            return "TRAINING";
        }
        if (containsAny(hint, AUDIT_HINTS)) {
            return "AUDIT";
        }
        return "DOCUMENT_CONTROL"; // produire la procédure / l'enregistrement manquant
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static String describeAction(ClauseGapFinding g) {
        StringBuilder sb = new StringBuilder();
        if (g.criticality() == MockAuditCriticality.MAJOR) {
            sb.append("Lever la non-conformité majeure sur la clause ");
        } else {
            sb.append("Traiter la non-conformité mineure sur la clause ");
        }
        sb.append(g.clauseCode()).append(" (").append(g.title()).append(") : ");
        if (g.coveredRequirements() == 0) {
            sb.append("mettre en place le dispositif requis et produire la preuve associée.");
        } else {
            sb.append("compléter la couverture (")
              .append(g.coveredRequirements()).append("/").append(g.totalRequirements())
              .append(") et fournir les preuves manquantes.");
        }
        return sb.toString();
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(java.util.Locale.ROOT);
    }
}
