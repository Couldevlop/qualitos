package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.util.List;
import java.util.Map;

/**
 * Résultat brut de la génération IA d'un audit blanc (Standards Hub §8.4
 * onglet 7) : les <b>questions ciblées</b> et les <b>constats par clause</b>
 * rédigés par l'IA, déjà projetés sur les clauses connues (anti-hallucination
 * côté ai-service). La criticité et le plan de remédiation restent calculés de
 * façon déterministe par le domaine (l'IA rédige, la règle tranche).
 *
 * <p>Value object PUR. {@code aiFindings} : clauseCode → texte du constat IA
 * (peut être incomplet ; le service complète par un constat déterministe).
 */
public record GeneratedMockAudit(
        List<MockAuditQuestion> questions,
        Map<String, String> aiFindings,
        double readiness,
        String provider) {

    public GeneratedMockAudit {
        questions = questions == null ? List.of() : List.copyOf(questions);
        aiFindings = aiFindings == null ? Map.of() : Map.copyOf(aiFindings);
        provider = provider == null ? "" : provider;
    }
}
