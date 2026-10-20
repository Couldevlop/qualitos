package com.openlab.qualitos.quality.spc.capability.domain;

import java.util.Map;

/**
 * Les constantes des cartes X̄ – R, par taille de sous-groupe.
 *
 * <p>Ce sont des données brutes de tables statistiques, pas des calculs : les
 * valeurs de n = 2 à 5 sont celles du classeur de référence
 * ({@code docs/spc/SPC Cp Cpk Study Template.xlsx}), arrondies comme lui ; au-delà,
 * les tables usuelles. Les reprendre à l'identique est ce qui fait qu'une étude
 * saisie ici donne le même Cpk que le classeur qu'elle remplace.
 *
 * @param d2 estimateur de sigma : σ = R̄ / d2
 * @param a2 limites de la carte X̄ : X̿ ± A2 × R̄
 * @param d3 limite basse de la carte R : D3 × R̄
 * @param d4 limite haute de la carte R : D4 × R̄
 */
public record SpcConstants(double d2, double a2, double d3, double d4) {

    public static final int MIN_SUBGROUP_SIZE = 2;
    public static final int MAX_SUBGROUP_SIZE = 10;

    private static final Map<Integer, SpcConstants> TABLE = Map.of(
            2, new SpcConstants(1.13, 1.88, 0, 3.27),
            3, new SpcConstants(1.69, 1.02, 0, 2.57),
            4, new SpcConstants(2.06, 0.73, 0, 2.29),
            5, new SpcConstants(2.33, 0.58, 0, 2.11),
            6, new SpcConstants(2.53, 0.48, 0, 2.00),
            7, new SpcConstants(2.70, 0.42, 0.076, 1.92),
            8, new SpcConstants(2.85, 0.37, 0.136, 1.86),
            9, new SpcConstants(2.97, 0.34, 0.184, 1.82),
            10, new SpcConstants(3.08, 0.31, 0.223, 1.78));

    /** Les constantes d'une taille de sous-groupe admise (2 à 10). */
    public static SpcConstants forSubgroupSize(int n) {
        SpcConstants c = TABLE.get(n);
        if (c == null) {
            throw new SpcValidationException("subgroupSize",
                    "La taille du sous-groupe va de " + MIN_SUBGROUP_SIZE + " à " + MAX_SUBGROUP_SIZE + ".");
        }
        return c;
    }
}
