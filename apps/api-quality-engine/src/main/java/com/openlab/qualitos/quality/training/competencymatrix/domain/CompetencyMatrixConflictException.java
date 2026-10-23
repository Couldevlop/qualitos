package com.openlab.qualitos.quality.training.competencymatrix.domain;

/** La personne, ou la compétence, figure déjà dans la matrice. */
public class CompetencyMatrixConflictException extends RuntimeException {

    private final String field;

    public CompetencyMatrixConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
