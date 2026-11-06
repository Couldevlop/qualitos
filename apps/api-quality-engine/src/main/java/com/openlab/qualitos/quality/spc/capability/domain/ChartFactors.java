package com.openlab.qualitos.quality.spc.capability.domain;

/**
 * Les facteurs de la carte retenue pour une taille d'échantillon donnée.
 *
 * <p>C'est le seul endroit qui tranche : une étude de {@code n} mesures par
 * sous-groupe se conduit sur la carte X̄ – R jusqu'à {@value #RANGE_CHART_MAX},
 * sur la carte X̄ – S au-delà, et n'est pas calculable passé
 * {@value #MAX_SUBGROUP_SIZE}. Le reste du calcul
 * ({@link CapabilityCalculation}) ne connaît plus que quatre nombres, les mêmes
 * quelle que soit la carte — ce qui évite d'écrire deux fois les formules de
 * capabilité et de se tromper une fois sur deux.
 *
 * <p><b>Pourquoi une borne et non un prolongement silencieux.</b> Avant,
 * l'écran s'arrêtait à dix parce que la table des constantes s'arrêtait à dix.
 * Lever la borne sans changer de carte aurait produit un Cpk à partir d'un d2
 * inexistant : un chiffre faux, présenté comme un résultat. Deux cartes et une
 * borne dite valent mieux qu'une carte et un chiffre inventé.
 *
 * @param subgroupSize     la taille d'échantillon, {@code n}
 * @param chart            la carte de dispersion retenue
 * @param sigmaDivisor     d2 (carte R) ou c4 (carte S) : σ̂ = dispersion moyenne / ce diviseur
 * @param meanFactor       A2 (carte R) ou A3 (carte S) : limites X̿ ± facteur × dispersion moyenne
 * @param dispersionLower  D3 (carte R) ou B3 (carte S)
 * @param dispersionUpper  D4 (carte R) ou B4 (carte S)
 */
public record ChartFactors(int subgroupSize, DispersionChart chart, double sigmaDivisor,
                           double meanFactor, double dispersionLower, double dispersionUpper) {

    public static final int MIN_SUBGROUP_SIZE = SpcConstants.MIN_SUBGROUP_SIZE;

    /** La dernière taille où l'étendue reste un estimateur recommandé de σ. */
    public static final int RANGE_CHART_MAX = SpcConstants.MAX_SUBGROUP_SIZE;

    /** La dernière taille tabulée, cartes R et S confondues. */
    public static final int MAX_SUBGROUP_SIZE = StdevConstants.MAX_SUBGROUP_SIZE;

    /**
     * Les facteurs d'une taille d'échantillon admise (2 à {@value #MAX_SUBGROUP_SIZE}).
     *
     * @throws SpcValidationException sur le champ {@code subgroupSize} si la
     *                                taille sort du domaine des deux cartes
     */
    public static ChartFactors forSubgroupSize(int n) {
        if (n < MIN_SUBGROUP_SIZE || n > MAX_SUBGROUP_SIZE) {
            throw new SpcValidationException("subgroupSize",
                    "La taille d'échantillon va de " + MIN_SUBGROUP_SIZE + " à " + MAX_SUBGROUP_SIZE
                            + " : au-delà, les constantes des cartes de contrôle ne sont pas tabulées.");
        }
        if (n <= RANGE_CHART_MAX) {
            SpcConstants k = SpcConstants.forSubgroupSize(n);
            return new ChartFactors(n, DispersionChart.RANGE, k.d2(), k.a2(), k.d3(), k.d4());
        }
        StdevConstants k = StdevConstants.forSubgroupSize(n);
        return new ChartFactors(n, DispersionChart.STDEV, k.c4(), k.a3(), k.b3(), k.b4());
    }

    /**
     * La dispersion d'un sous-groupe complet, dans l'unité de la carte retenue :
     * l'étendue max − min (carte R), ou l'écart-type d'échantillon s, diviseur
     * {@code n − 1} (carte S).
     *
     * @param values les {@code n} mesures du sous-groupe, dans leur ordre de relevé
     */
    public double dispersionOf(double[] values) {
        if (chart == DispersionChart.RANGE) {
            double min = values[0];
            double max = values[0];
            for (double v : values) {
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
            return max - min;
        }
        double somme = 0;
        for (double v : values) {
            somme += v;
        }
        double moyenne = somme / values.length;
        double carres = 0;
        for (double v : values) {
            carres += (v - moyenne) * (v - moyenne);
        }
        return Math.sqrt(carres / (values.length - 1));
    }
}
