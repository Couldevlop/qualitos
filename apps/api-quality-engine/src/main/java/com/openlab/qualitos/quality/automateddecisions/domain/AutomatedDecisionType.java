package com.openlab.qualitos.quality.automateddecisions.domain;

/**
 * Typologie de décision automatisée (RGPD Art. 22).
 * <ul>
 *   <li>PROFILING_ONLY — profilage sans décision automatisée (Art. 4.4).
 *       Mécanisme d'opposition obligatoire (Art. 21).</li>
 *   <li>AUTOMATED_DECISION — décision automatisée sans effet juridique majeur.
 *       Mécanisme de révision humaine recommandé.</li>
 *   <li>AUTOMATED_DECISION_WITH_LEGAL_EFFECT — décision automatisée produisant
 *       des effets juridiques ou affectant significativement la personne
 *       (Art. 22.1). Interdite sauf : consentement explicite, nécessité
 *       contractuelle, ou autorisation par le droit de l'Union/État membre
 *       (Art. 22.2). Mécanisme de révision humaine obligatoire (Art. 22.3).</li>
 * </ul>
 */
public enum AutomatedDecisionType {
    PROFILING_ONLY,
    AUTOMATED_DECISION,
    AUTOMATED_DECISION_WITH_LEGAL_EFFECT;

    public boolean requiresHumanReview() {
        return this == AUTOMATED_DECISION_WITH_LEGAL_EFFECT;
    }
}
