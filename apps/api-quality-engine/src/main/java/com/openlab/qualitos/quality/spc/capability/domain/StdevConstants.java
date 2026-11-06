package com.openlab.qualitos.quality.spc.capability.domain;

/**
 * Les constantes de la carte X̄ – S, pour un sous-groupe de {@code n} mesures.
 *
 * <p><b>Elles sont CALCULÉES, pas recopiées.</b> Contrairement à d2, A2, D3 et
 * D4 ({@link SpcConstants}), qui viennent du classeur de référence avec ses
 * arrondis, c4, A3, B3 et B4 ont une définition fermée exacte :
 *
 * <pre>
 *   c4(n) = √(2 / (n − 1)) × Γ(n / 2) / Γ((n − 1) / 2)
 *   A3(n) = 3 / (c4 × √n)
 *   B3(n) = max(0, 1 − 3 √(1 − c4²) / c4)
 *   B4(n) = 1 + 3 √(1 − c4²) / c4
 * </pre>
 *
 * <p>Les tables publiées (Montgomery, <i>Introduction to Statistical Quality
 * Control</i>, annexe VI ; ASTM STP-15D) ne sont que l'impression arrondie de
 * ces formules. Les calculer plutôt que les recopier supprime le risque de
 * transcription sur quinze lignes de quatre colonnes — et c'est vérifiable :
 * {@code StdevConstantsTest} compare les valeurs calculées aux valeurs
 * publiées pour n = 2, 5, 10, 11, 12, 15, 20 et 25.
 *
 * <p>Γ ne demande aucune bibliothèque ici : {@code n} est entier, donc n/2 est
 * entier ou demi-entier, et Γ se déroule par récurrence depuis Γ(1) = 1 et
 * Γ(1/2) = √π.
 *
 * @param c4 estimateur de sigma : σ̂ = s̄ / c4 (s̄ sans biais une fois divisé par c4)
 * @param a3 limites de la carte X̄ : X̿ ± A3 × s̄
 * @param b3 limite basse de la carte S : B3 × s̄
 * @param b4 limite haute de la carte S : B4 × s̄
 */
public record StdevConstants(double c4, double a3, double b3, double b4) {

    /** Un sous-groupe de moins de deux mesures n'a pas de dispersion. */
    public static final int MIN_SUBGROUP_SIZE = 2;

    /**
     * Au-delà, l'écran refuse : c'est la dernière ligne des tables publiées.
     *
     * <p>La formule, elle, se prolongerait sans broncher — mais une carte de
     * contrôle dont personne ne peut recouper les facteurs dans une table n'est
     * pas auditable, et un sous-groupe de plus de vingt-cinq mesures relève du
     * contrôle à 100 %, pas de l'échantillonnage.
     */
    public static final int MAX_SUBGROUP_SIZE = 25;

    /** Les constantes de la carte X̄ – S pour {@code n} mesures par sous-groupe. */
    public static StdevConstants forSubgroupSize(int n) {
        if (n < MIN_SUBGROUP_SIZE || n > MAX_SUBGROUP_SIZE) {
            throw new SpcValidationException("subgroupSize",
                    "La carte X̄ – S va de " + MIN_SUBGROUP_SIZE + " à " + MAX_SUBGROUP_SIZE
                            + " mesures par sous-groupe.");
        }
        double c4 = c4(n);
        double spread = 3 * Math.sqrt(1 - c4 * c4) / c4;
        return new StdevConstants(c4, 3 / (c4 * Math.sqrt(n)), Math.max(0, 1 - spread), 1 + spread);
    }

    /** c4(n) = √(2 / (n − 1)) × Γ(n / 2) / Γ((n − 1) / 2). */
    static double c4(int n) {
        return Math.sqrt(2.0 / (n - 1)) * gammaHalf(n) / gammaHalf(n - 1);
    }

    /**
     * Γ(k / 2) pour un entier {@code k ≥ 1}, par récurrence exacte :
     * Γ(1) = 1, Γ(1/2) = √π, puis Γ(x + 1) = x Γ(x).
     */
    static double gammaHalf(int k) {
        boolean entier = k % 2 == 0;
        double valeur = entier ? 1.0 : Math.sqrt(Math.PI);
        double cible = k / 2.0;
        for (double x = entier ? 1.0 : 0.5; x < cible; x += 1.0) {
            valeur *= x;
        }
        return valeur;
    }
}
