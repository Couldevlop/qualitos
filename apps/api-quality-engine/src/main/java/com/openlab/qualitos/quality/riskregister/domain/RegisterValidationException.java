package com.openlab.qualitos.quality.riskregister.domain;

/**
 * Une fiche incomplète ou incohérente.
 *
 * <p>Porte le champ fautif : l'écran l'accroche au bon contrôle du formulaire.
 */
public class RegisterValidationException extends RuntimeException {

    private final String field;

    public RegisterValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
