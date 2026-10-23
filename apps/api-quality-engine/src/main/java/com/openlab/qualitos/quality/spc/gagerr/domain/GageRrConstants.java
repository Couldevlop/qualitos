package com.openlab.qualitos.quality.spc.gagerr.domain;

import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;

import java.util.Map;

/**
 * Les constantes de la méthode « moyenne et étendue » (AIAG, MSA 4e éd.).
 *
 * <p>Données brutes de la table de référence, reprises de l'onglet
 * « Constantes » du classeur {@code docs/spc/Etude_RR_Gage_1.xlsx}, pour un
 * multiplicateur de 5,15 σ. Avec 6 σ, K1, K2 et K3 sont mises à l'échelle
 * (× 6 / 5,15) ; D4 ne bouge pas, il borne une étendue, pas une dispersion.
 *
 * <p>Le domaine de la table fixe le plan admis : 2 ou 3 opérateurs, 2 ou 3
 * essais, 2 à 10 pièces.
 *
 * @param k1 répétabilité : EV = R̄̄ × K1
 * @param k2 reproductibilité : AV = √[(X̄diff × K2)² − EV² / (n × r)]
 * @param k3 variation pièces : PV = Rp × K3
 * @param d4 limite des étendues : UCL<sub>R</sub> = D4 × R̄̄
 */
public record GageRrConstants(double k1, double k2, double k3, double d4) {

    /** Le multiplicateur de référence de la table (99 % de la dispersion). */
    public static final double REFERENCE_MULTIPLIER = 5.15;
    /** L'autre multiplicateur admis (99,73 %). */
    public static final double SIX_SIGMA = 6.0;

    public static final int MIN_OPERATORS = 2;
    public static final int MAX_OPERATORS = 3;
    public static final int MIN_PARTS = 2;
    public static final int MAX_PARTS = 10;
    public static final int MIN_TRIALS = 2;
    public static final int MAX_TRIALS = 3;

    private static final Map<Integer, Double> K1 = Map.of(2, 4.56, 3, 3.05);
    private static final Map<Integer, Double> K2 = Map.of(2, 3.65, 3, 2.70);
    private static final Map<Integer, Double> K3 = Map.of(
            2, 3.65, 3, 2.70, 4, 2.30, 5, 2.08, 6, 1.93, 7, 1.82, 8, 1.74, 9, 1.67, 10, 1.62);
    private static final Map<Integer, Double> D4 = Map.of(2, 3.27, 3, 2.58);

    /**
     * Les constantes d'un plan k opérateurs × n pièces × r essais, ajustées au
     * multiplicateur choisi. Un plan hors table est refusé, champ par champ.
     */
    public static GageRrConstants forPlan(int operators, int parts, int trials, double multiplier) {
        if (!K2.containsKey(operators)) {
            throw new SpcValidationException("operatorCount",
                    "Le nombre d'opérateurs va de " + MIN_OPERATORS + " à " + MAX_OPERATORS + ".");
        }
        if (!K3.containsKey(parts)) {
            throw new SpcValidationException("partCount",
                    "Le nombre de pièces va de " + MIN_PARTS + " à " + MAX_PARTS + ".");
        }
        if (!K1.containsKey(trials)) {
            throw new SpcValidationException("trialCount",
                    "Le nombre d'essais va de " + MIN_TRIALS + " à " + MAX_TRIALS + ".");
        }
        requireMultiplier(multiplier);
        double echelle = multiplier / REFERENCE_MULTIPLIER;
        return new GageRrConstants(K1.get(trials) * echelle, K2.get(operators) * echelle,
                K3.get(parts) * echelle, D4.get(trials));
    }

    /** 5,15 σ ou 6 σ : les deux seules lectures de la table. */
    public static double requireMultiplier(double multiplier) {
        if (Math.abs(multiplier - REFERENCE_MULTIPLIER) < 1e-9) {
            return REFERENCE_MULTIPLIER;
        }
        if (Math.abs(multiplier - SIX_SIGMA) < 1e-9) {
            return SIX_SIGMA;
        }
        throw new SpcValidationException("sigmaMultiplier", "Le multiplicateur σ vaut 5,15 ou 6.");
    }
}
