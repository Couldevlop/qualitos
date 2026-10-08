package com.openlab.qualitos.quality.circuit.domain;

/**
 * Une étape de circuit refusée, avec sa raison.
 *
 * <p>{@link Reason#NOT_YOUR_STEP} se lit 403 (l'utilisateur n'a pas le rôle de
 * l'étape) ; {@link Reason#INVALID} 422 (circuit mal formé) ; les autres 409 :
 * la demande est correcte, mais l'état du circuit ne la permet pas.
 */
public class CircuitException extends RuntimeException {

    public enum Reason {
        INVALID,
        UNKNOWN_SUBJECT,
        NOT_IN_PROGRESS,
        AUTHOR_CANNOT_DECIDE,
        ALREADY_DECIDED,
        NOT_YOUR_STEP
    }

    private final Reason reason;
    private final String field;

    public CircuitException(Reason reason, String message) {
        this(reason, null, message);
    }

    public CircuitException(Reason reason, String field, String message) {
        super(message);
        this.reason = reason;
        this.field = field;
    }

    public Reason getReason() {
        return reason;
    }

    public String getField() {
        return field;
    }
}
