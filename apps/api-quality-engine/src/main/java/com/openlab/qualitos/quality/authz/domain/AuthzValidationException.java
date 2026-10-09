package com.openlab.qualitos.quality.authz.domain;

/** Une saisie de rôle ou d'attribution refusée, rattachée au champ fautif (422). */
public class AuthzValidationException extends RuntimeException {

    private final String field;

    public AuthzValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
