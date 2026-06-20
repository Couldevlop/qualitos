package com.openlab.qualitos.quality.standards.auditblanc.domain;

import java.util.UUID;

/**
 * Exécution d'audit blanc introuvable (ou hors du tenant courant — le 404
 * masque l'existence cross-tenant, OWASP A01). Standards Hub §8.4 onglet 7.
 */
public class MockAuditRunNotFoundException extends RuntimeException {

    public MockAuditRunNotFoundException(UUID id) {
        super("Mock audit run not found: " + id);
    }
}
