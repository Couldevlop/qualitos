package com.openlab.qualitos.quality.fivewhys;

/** Règle de la méthode non respectée (chaîne trop longue, conclusion trop tôt…). */
public class FiveWhysStateException extends RuntimeException {
    public FiveWhysStateException(String message) {
        super(message);
    }
}
