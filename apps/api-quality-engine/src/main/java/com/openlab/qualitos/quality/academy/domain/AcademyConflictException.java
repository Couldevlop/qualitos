package com.openlab.qualitos.quality.academy.domain;

/**
 * Conflit d'unicité métier (ex. code de cours déjà pris, double inscription).
 * Mappée en 409.
 */
public class AcademyConflictException extends RuntimeException {

    public AcademyConflictException(String message) {
        super(message);
    }
}
