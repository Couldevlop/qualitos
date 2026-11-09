package com.openlab.qualitos.quality.settings.catalog;

/**
 * Un catalogue refuse de s'enregistrer : code en double, code hors format, liste
 * vide, ou tentative d'ajouter une valeur à une liste dont les codes sont ceux du
 * code ({@link CatalogMode#RENAME}). Rendu en 422.
 */
public class CatalogValidationException extends RuntimeException {

    public CatalogValidationException(String message) {
        super(message);
    }
}
