package com.openlab.qualitos.quality.capa;

import java.util.UUID;

/** Pièce de preuve absente du dossier, ou d'un autre tenant. Mappée en 404. */
public class CapaEvidenceNotFoundException extends RuntimeException {
    public CapaEvidenceNotFoundException(UUID id) {
        super("CAPA evidence not found: " + id);
    }
}
