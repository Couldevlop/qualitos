package com.openlab.qualitos.quality.fmeascale;

import java.util.Objects;

/**
 * Une ligne de barème : ce que vaut un score, et pourquoi.
 *
 * <p>Objet de valeur, sans identité : deux lignes de même score et de même
 * texte sont la même règle de cotation, quel que soit le tenant qui l'a écrite.
 *
 * <p>{@code timePeriod} et {@code failureRate} n'ont de sens que pour
 * l'occurrence — « une fois par semaine », « 1 sur 8 ». Nuls ailleurs, plutôt
 * que trois types qui ne différeraient que par deux champs.
 */
public record FmeaScaleRow(int score, String label, String description,
                           String timePeriod, String failureRate) {

    /** Score minimal : l'effet le plus anodin, la défaillance la plus rare. */
    public static final int MIN_SCORE = 1;

    /** Score maximal : le danger, le quotidien, l'indétectable. */
    public static final int MAX_SCORE = 10;

    private static final int MAX_LABEL = 120;
    private static final int MAX_TEXT = 500;

    public FmeaScaleRow {
        if (score < MIN_SCORE || score > MAX_SCORE) {
            // Le RPN est le produit des trois cotations : un score hors bornes
            // le ferait sortir de la plage 1-1000 sans qu'aucun écran ne le dise.
            throw new IllegalArgumentException(
                    "Score de barème hors de 1..10 : " + score);
        }
        label = requireLabel(label);
        description = bounded(description, MAX_TEXT, "description");
        timePeriod = bounded(timePeriod, MAX_LABEL, "timePeriod");
        failureRate = bounded(failureRate, MAX_LABEL, "failureRate");
    }

    /** Une ligne de sévérité ou de détection. */
    public static FmeaScaleRow of(int score, String label, String description) {
        return new FmeaScaleRow(score, label, description, null, null);
    }

    /** Une ligne d'occurrence, avec sa période et son taux. */
    public static FmeaScaleRow occurrence(int score, String label, String timePeriod,
                                          String failureRate) {
        return new FmeaScaleRow(score, label, null, timePeriod, failureRate);
    }

    /**
     * Un intitulé vide rendrait la ligne illisible dans le barème — et un barème
     * qu'on ne lit pas ne sert à rien. Il est donc exigé, à la différence de tout
     * le reste.
     */
    private static String requireLabel(String label) {
        String trimmed = label == null ? "" : label.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Intitulé de barème vide");
        }
        if (trimmed.length() > MAX_LABEL) {
            throw new IllegalArgumentException(
                    "Intitulé de barème au-delà de " + MAX_LABEL + " caractères");
        }
        return trimmed;
    }

    /**
     * Refuse plutôt que tronquer : une description de cotation coupée au milieu
     * se lit comme une règle complète, et fait coter faux.
     */
    private static String bounded(String value, int max, String field) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(
                    "Champ de barème '" + field + "' au-delà de " + max + " caractères");
        }
        return trimmed;
    }

    /** Vrai si les deux lignes disent la même chose, au blanc près. */
    public boolean saysTheSameAs(FmeaScaleRow other) {
        return other != null
                && score == other.score
                && Objects.equals(label, other.label)
                && Objects.equals(description, other.description)
                && Objects.equals(timePeriod, other.timePeriod)
                && Objects.equals(failureRate, other.failureRate);
    }
}
