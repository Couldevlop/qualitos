package com.openlab.qualitos.quality.change.domain;

import java.util.Map;

/**
 * Le score d'impact d'une demande et sa classification.
 *
 * <p>score = arrondi( Σ(points_d × poids_d) / Σ(3 × poids_d) × 100 ), points :
 * Aucun 0, Faible 1, Moyen 2, Élevé 3. Avec les poids par défaut (tous à 1),
 * c'est la somme des points sur 24 : la maquette (13 / 24) donne 54.
 *
 * <p>Tant qu'un domaine n'est pas évalué, il n'y a ni score ni classification :
 * « non évalué ». Un domaine de poids 0 doit être évalué comme les autres mais
 * ne pèse pas.
 */
public final class MocScoring {

    private MocScoring() {}

    /** Le résultat : complet ou non, et le cas échéant son score et sa classe. */
    public record Assessment(boolean complete, Integer score, MocClassification classification) {

        static final Assessment INCOMPLETE = new Assessment(false, null, MocClassification.NOT_ASSESSED);
    }

    public static Assessment evaluate(Map<ImpactDomain, ImpactLevel> levels, MocWeights weights,
                                      MocThresholds thresholds) {
        long obtenus = 0;
        long possibles = 0;
        for (ImpactDomain d : ImpactDomain.values()) {
            ImpactLevel level = levels == null ? null : levels.get(d);
            if (level == null) {
                return Assessment.INCOMPLETE;
            }
            int w = weights.weightOf(d);
            obtenus += (long) level.points() * w;
            possibles += (long) ImpactLevel.MAX_POINTS * w;
        }
        int score = (int) Math.round(obtenus * 100.0 / possibles);
        return new Assessment(true, score, thresholds.classify(score));
    }
}
