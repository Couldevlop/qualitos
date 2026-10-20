package com.openlab.qualitos.quality.spc.capability.domain;

/**
 * Une étude SPC incomplète ou incohérente.
 *
 * <p>Porte le champ fautif : l'écran le rattache au bon contrôle du formulaire
 * plutôt que d'afficher un message flottant.
 */
public class SpcValidationException extends RuntimeException {

    private final String field;

    public SpcValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
