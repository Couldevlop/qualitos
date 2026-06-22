package com.openlab.qualitos.quality.academy.domain;

/**
 * Ressource d'académie introuvable (cours, module, leçon, quiz, inscription,
 * certificat) — ou hors du tenant courant. Mappée en 404.
 */
public class AcademyNotFoundException extends RuntimeException {

    public AcademyNotFoundException(String resource, Object id) {
        super(resource + " not found: " + id);
    }
}
