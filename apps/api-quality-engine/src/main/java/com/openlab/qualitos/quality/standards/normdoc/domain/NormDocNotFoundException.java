package com.openlab.qualitos.quality.standards.normdoc.domain;

import java.util.UUID;

/** Document normatif généré introuvable (dans le tenant courant). */
public class NormDocNotFoundException extends RuntimeException {
    public NormDocNotFoundException(UUID id) {
        super("Normative document not found: " + id);
    }
}
