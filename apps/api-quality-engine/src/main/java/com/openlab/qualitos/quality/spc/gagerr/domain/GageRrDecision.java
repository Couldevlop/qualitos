package com.openlab.qualitos.quality.spc.gagerr.domain;

/**
 * Le verdict AIAG sur le %GRR (part du système de mesure dans la variation
 * totale) : la règle de la cellule C38 de l'onglet « Résultats ».
 */
public enum GageRrDecision {
    /** %GRR sous 10 % : le système de mesure est acceptable. */
    ACCEPTABLE,
    /** %GRR de 10 % à 30 % inclus : acceptable sous conditions, décision à justifier. */
    CONDITIONAL,
    /** %GRR au-delà de 30 % : améliorer le système de mesure. */
    NOT_ACCEPTABLE;

    public static final double ACCEPTABLE_BELOW = 0.10;
    public static final double CONDITIONAL_UP_TO = 0.30;

    /** @param grrRatio GRR / TV, en fraction (0,21 pour 21 %) */
    public static GageRrDecision of(double grrRatio) {
        if (grrRatio < ACCEPTABLE_BELOW) {
            return ACCEPTABLE;
        }
        return grrRatio <= CONDITIONAL_UP_TO ? CONDITIONAL : NOT_ACCEPTABLE;
    }
}
