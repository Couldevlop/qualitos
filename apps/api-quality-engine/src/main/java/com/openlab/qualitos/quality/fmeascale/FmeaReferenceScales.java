package com.openlab.qualitos.quality.fmeascale;

import java.util.List;

/**
 * Le bareme de cotation de reference, celui qu'un tenant recoit tant qu'il n'a
 * rien redefini.
 *
 * <p>Il vit dans le CODE et non en base : ce n'est pas une donnee de tenant,
 * c'est le point de depart commun. Le semer en base a la creation de chaque
 * tenant l'aurait fige au jour de l'inscription, et aurait rendu impossible de
 * distinguer « jamais touche » de « redefini a l'identique » - la distinction
 * meme qu'un auditeur vient chercher.
 *
 * <p>Reproduit dans la langue du referentiel d'origine (anglais). Une echelle de
 * cotation traduite librement n'est plus la meme echelle ; un tenant qui veut la
 * sienne, dans sa langue, la redefinit - c'est exactement ce que permet ce
 * module.
 *
 * <p>Source : classeur de reference qualite, feuille 4.
 */
public final class FmeaReferenceScales {

    private FmeaReferenceScales() {}

    /** Severite : gravite de l'effet pour le client, de 10 (le plus critique) a 1. */
    public static final List<FmeaScaleRow> SEVERITY = List.of(
            FmeaScaleRow.of(10, "Hazardous - Without Warning", "May expose client to loss or harm without warning."),
            FmeaScaleRow.of(9, "Hazardous - With Warning", "May expose client to loss or harm with some warning."),
            FmeaScaleRow.of(8, "Very High", "Will cause major disruption of service directly affecting a client."),
            FmeaScaleRow.of(7, "High", "Minor disruption of service directly affecting a client."),
            FmeaScaleRow.of(6, "Moderate", "Major disruption of service not involving a client directly."),
            FmeaScaleRow.of(5, "Low", "Minor disruption of service not involving a client."),
            FmeaScaleRow.of(4, "Very Low", "Minor disruption of service involving client that doesn't require reworking or inconvenience to client."),
            FmeaScaleRow.of(3, "Minor", "Minor disruption of service not involving client that doesn't require reworking or inconvenience to client."),
            FmeaScaleRow.of(2, "Very Minor", "No disruption of service noticed by client, no rework necessary."),
            FmeaScaleRow.of(1, "None", "No Effect")
    );

    /** Detection : chance de reperer la defaillance, de 10 (le plus critique) a 1. */
    public static final List<FmeaScaleRow> DETECTION = List.of(
            FmeaScaleRow.of(10, "Nearly impossible", "No current way to detect failure"),
            FmeaScaleRow.of(9, "Very Remote", "Very remote likelihood of detecting failure."),
            FmeaScaleRow.of(8, "Remote", "Remote likelihood of detecting failure."),
            FmeaScaleRow.of(7, "Very Low", "Very low likelihood of detecting failure."),
            FmeaScaleRow.of(6, "Low", "Low likelihood of detecting failure."),
            FmeaScaleRow.of(5, "Moderate", "Moderate likelihood of detecting failure."),
            FmeaScaleRow.of(4, "Moderately High", "Moderately high likelihood of detecting failure."),
            FmeaScaleRow.of(3, "High", "High likelihood of detecting failure."),
            FmeaScaleRow.of(2, "Very High", "Very high likelihood of detecting failure."),
            FmeaScaleRow.of(1, "Nearly Certain", "Near certain likelihood of detecting failure.")
    );

    /**
     * Occurrence : frequence attendue de la defaillance, de 10 (le plus
     * critique) a 1.
     *
     * <p>Le classeur FUSIONNE l'intitule sur plusieurs scores - « Moderate »
     * couvre 6, 5 et 4. Il est recopie sur chaque ligne : un score sans nom ne
     * se cote pas, et un tenant ne pourrait pas adopter tel quel un bareme dont
     * la moitie des cases est vide.
     */
    public static final List<FmeaScaleRow> OCCURRENCE = List.of(
            FmeaScaleRow.occurrence(10, "Very High", "More than once per day", "> 1 in 2"),
            FmeaScaleRow.occurrence(9, "Very High", "Once every 3-4 days", "1 in 3000"),
            FmeaScaleRow.occurrence(8, "High", "Once every week", "1 in 8"),
            FmeaScaleRow.occurrence(7, "High", "Once every month", "1 in 20"),
            FmeaScaleRow.occurrence(6, "Moderate", "Once every 3 months", "1 in 800"),
            FmeaScaleRow.occurrence(5, "Moderate", "Once every 6 months", "1 in 400"),
            FmeaScaleRow.occurrence(4, "Moderate", "Once a year", "1 in 800"),
            FmeaScaleRow.occurrence(3, "Low", "Once every 1 - 3 years", "1 in 1500"),
            FmeaScaleRow.occurrence(2, "Very Low", "Once every 3 - 6 years", "1 in 3000"),
            FmeaScaleRow.occurrence(1, "Remote", "Once Every 7+ Years", "1 in 6000")
    );

    /** Le bareme de reference d'une echelle donnee. */
    public static List<FmeaScaleRow> of(FmeaScaleKind kind) {
        return switch (kind) {
            case SEVERITY -> SEVERITY;
            case OCCURRENCE -> OCCURRENCE;
            case DETECTION -> DETECTION;
        };
    }
}
