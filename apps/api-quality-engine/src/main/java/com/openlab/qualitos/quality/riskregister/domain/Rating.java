package com.openlab.qualitos.quality.riskregister.domain;

/**
 * Une cotation : deux notes de 1 à 5 multipliées.
 *
 * <p>Gravité × probabilité pour un risque, gain attendu × faisabilité pour une
 * opportunité. Le produit va de 1 à 25 ; le niveau qu'on en tire dépend du
 * registre ({@link RiskLevel}, {@link OpportunityLevel}).
 */
public record Rating(int first, int second) {

    public static final int MIN = 1;
    public static final int MAX = 5;

    /**
     * @param firstField  nom du champ porté par la première note, pour le 422
     * @param secondField nom du champ porté par la seconde
     */
    public static Rating of(Integer first, Integer second, String firstField, String secondField) {
        return new Rating(note(first, firstField), note(second, secondField));
    }

    public int score() {
        return first * second;
    }

    /** « 4x3 » : la forme que garde le journal de suivi, lue par l'écran. */
    public String code() {
        return first + "x" + second;
    }

    private static int note(Integer value, String field) {
        if (value == null) {
            throw new RegisterValidationException(field, "La note est obligatoire.");
        }
        if (value < MIN || value > MAX) {
            throw new RegisterValidationException(field, "La note va de 1 à 5.");
        }
        return value;
    }
}
