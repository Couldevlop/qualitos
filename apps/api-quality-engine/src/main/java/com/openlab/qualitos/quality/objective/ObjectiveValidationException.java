package com.openlab.qualitos.quality.objective;

/** Ce que l'objectif decrit ne tient pas debout : 422, pas 500. */
public class ObjectiveValidationException extends RuntimeException {

    public ObjectiveValidationException(String message) { super(message); }
}
