package com.openlab.qualitos.quality.costofquality.domain;

/**
 * Une ligne de coût incomplète ou incohérente.
 *
 * <p>Porte le champ fautif : l'écran le rattache au bon contrôle du formulaire
 * plutôt que d'afficher un message flottant.
 */
public class CoqValidationException extends RuntimeException {

    private final String field;

    public CoqValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
