package com.openlab.qualitos.quality.capa.effectiveness.domain;

/**
 * La fenêtre d'observation d'une CAPA : la durée pendant laquelle on regarde si
 * le problème revient, et la durée symétrique qu'on regarde avant l'ouverture du
 * dossier.
 *
 * <p><b>Un mois vaut trente jours.</b> C'est une approximation, et elle est
 * assumée : la fenêtre ne sert pas à dater un événement mais à comparer deux
 * périodes de MÊME durée. Compter les vrais mois calendaires rendrait la
 * comparaison inégale — février contre juillet — pour un résultat qui ne
 * changerait rien à la lecture.
 */
public record EffectivenessWindow(int months) {

    private static final int DAYS_PER_MONTH = 30;
    private static final int MAX_MONTHS = 24;

    public EffectivenessWindow {
        if (months < 1 || months > MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "Fenêtre d'observation hors bornes (1 à " + MAX_MONTHS + " mois) : " + months);
        }
    }

    public static EffectivenessWindow ofMonths(int months) {
        return new EffectivenessWindow(months);
    }

    public int days() {
        return months * DAYS_PER_MONTH;
    }
}
