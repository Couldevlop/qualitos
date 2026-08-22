package com.openlab.qualitos.quality.revisionrequests.domain;

import java.util.OptionalInt;

/**
 * Traduit un nombre de non-conformités observées sur douze mois glissants en cote
 * d'occurrence FMEA (1 à 10).
 *
 * <p>La table AIAG se lit en défauts par million d'opportunités. La plateforme n'a
 * ni ordre de fabrication ni quantité lancée : compter les NC est l'approximation
 * honnête, et elle est nommée comme telle dans la justification affichee.
 *
 * <p>Seuils volontairement exposés en constante : un ingénieur qualité les discute,
 * et le jour où ils devront varier par tenant, c'est cette table qui deviendra un
 * paramètre — pas la logique autour.
 */
public final class OccurrenceProposalCalculator {

    /** Borne haute (incluse) du nombre de NC pour chaque cote, de la cote 4 à la cote 9. */
    private static final int[] UPPER_BOUNDS = {1, 2, 4, 7, 12, 20};
    private static final int FIRST_RATING = 4;

    private OccurrenceProposalCalculator() {}

    public static int ratingFor(int ncCountOverTwelveMonths) {
        int count = Math.max(0, ncCountOverTwelveMonths);
        if (count == 0) return 1;
        for (int i = 0; i < UPPER_BOUNDS.length; i++) {
            if (count <= UPPER_BOUNDS[i]) return FIRST_RATING + i;
        }
        return 10;
    }

    /**
     * La cote à proposer, ou rien s'il n'y a rien à dire. Une NC ne fait jamais
     * baisser une cote : un défaut survenu ne minore pas un risque.
     */
    public static OptionalInt proposal(int currentRating, int ncCountOverTwelveMonths) {
        int computed = ratingFor(ncCountOverTwelveMonths);
        return computed > currentRating ? OptionalInt.of(computed) : OptionalInt.empty();
    }
}
