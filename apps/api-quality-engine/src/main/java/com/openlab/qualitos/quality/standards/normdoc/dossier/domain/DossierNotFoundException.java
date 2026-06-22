package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import java.util.UUID;

/**
 * Dossier introuvable (ou hors tenant — l'existence cross-tenant est masquée par
 * un 404, OWASP A01).
 */
public class DossierNotFoundException extends RuntimeException {
    public DossierNotFoundException(UUID id) {
        super("Documentation dossier not found: " + id);
    }
}
