package com.openlab.qualitos.quality.training.competencymatrix.domain;

/**
 * L'opérateur ou la compétence n'est pas dans la matrice — ou pas dans celle de
 * ce client, ce qui revient au même : on ne dit pas à un client ce qui existe
 * chez un autre.
 */
public class CompetencyMatrixNotFoundException extends RuntimeException {

    public CompetencyMatrixNotFoundException(String message) {
        super(message);
    }
}
