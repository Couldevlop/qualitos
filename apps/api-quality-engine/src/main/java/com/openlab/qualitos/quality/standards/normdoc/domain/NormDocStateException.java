package com.openlab.qualitos.quality.standards.normdoc.domain;

/** Transition de cycle de vie illégale sur un document normatif (→ 409). */
public class NormDocStateException extends RuntimeException {
    public NormDocStateException(String message) {
        super(message);
    }
}
