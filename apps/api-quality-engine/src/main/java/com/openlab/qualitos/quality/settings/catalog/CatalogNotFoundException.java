package com.openlab.qualitos.quality.settings.catalog;

/** Aucune liste ne porte ce nom : elle n'existe pas, ou n'est pas ouverte au paramétrage. */
public class CatalogNotFoundException extends RuntimeException {

    public CatalogNotFoundException(String slug) {
        super("Unknown settings catalogue: " + slug);
    }
}
