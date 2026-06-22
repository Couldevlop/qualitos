package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

/** Transition / opération invalide sur un dossier documentaire (→ HTTP 409). */
public class DossierStateException extends RuntimeException {
    public DossierStateException(String message) {
        super(message);
    }
}
