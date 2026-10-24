package com.openlab.qualitos.quality.dmaic.resolution.domain;

/**
 * Une saisie bien formée mais refusée par la règle (422). {@code field} dit
 * laquelle, pour que l'écran l'accroche au bon contrôle.
 */
public class ResolutionValidationException extends RuntimeException {

    private final String field;

    public ResolutionValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
