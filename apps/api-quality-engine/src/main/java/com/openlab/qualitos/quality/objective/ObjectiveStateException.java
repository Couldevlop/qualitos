package com.openlab.qualitos.quality.objective;

/** Le geste demande ne va pas avec l'etat courant : 409. */
public class ObjectiveStateException extends RuntimeException {

    public ObjectiveStateException(String message) { super(message); }
}
