package com.openlab.qualitos.quality.training.competencymatrix.domain;

/**
 * Une saisie de la matrice bien formée mais refusée par la règle métier :
 * atelier inconnu, niveau hors échelle, liste d'ateliers en double…
 *
 * <p>Porte le champ fautif : l'écran le rattache au bon contrôle.
 */
public class CompetencyMatrixValidationException extends RuntimeException {

    private final String field;

    public CompetencyMatrixValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
