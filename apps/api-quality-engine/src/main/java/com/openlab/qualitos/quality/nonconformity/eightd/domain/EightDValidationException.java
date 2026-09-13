package com.openlab.qualitos.quality.nonconformity.eightd.domain;

/** Une saisie refusée sur le fond (longueur). Rendu en 422. */
public class EightDValidationException extends RuntimeException {

    public EightDValidationException(String message) {
        super(message);
    }
}
