package com.openlab.qualitos.quality.objective;

import java.util.Arrays;

/**
 * La période sur laquelle un objectif se compare à son KPI : 1, 3, 6 ou 12 mois.
 *
 * <p>Fermée à ces quatre valeurs, et pas libre. La période est l'unité dans
 * laquelle se comptent les dépassements consécutifs — « deux périodes au-delà du
 * seuil d'alerte ». Une période libre rendrait cette règle incomparable d'un
 * objectif à l'autre, et un objectif réglé sur sept semaines ne voudrait rien
 * dire devant une revue trimestrielle.
 *
 * <p>Ce n'est pas la fréquence du KPI ({@code KpiFrequency}) : un indicateur
 * relevé chaque mois peut servir un objectif revu au trimestre. La fréquence dit
 * à quel rythme on mesure, la période à quel rythme on juge.
 */
public enum ObjectivePeriod {

    MONTHLY(1),
    QUARTERLY(3),
    HALF_YEARLY(6),
    YEARLY(12);

    private final int months;

    ObjectivePeriod(int months) { this.months = months; }

    public int months() { return months; }

    /**
     * La période qui compte ce nombre de mois.
     *
     * @throws IllegalArgumentException si le nombre n'est pas 1, 3, 6 ou 12 —
     *         le même refus que la contrainte {@code ck_quality_objectives_period}
     *         en base, pour que l'API ne laisse pas passer ce que la base
     *         refusera de toute façon.
     */
    public static ObjectivePeriod ofMonths(int months) {
        return Arrays.stream(values())
                .filter(p -> p.months == months)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "La période d'un objectif est de 1, 3, 6 ou 12 mois : " + months));
    }

    /** {@code true} si ce nombre de mois est une période admise. */
    public static boolean isValidMonths(int months) {
        return Arrays.stream(values()).anyMatch(p -> p.months == months);
    }
}
