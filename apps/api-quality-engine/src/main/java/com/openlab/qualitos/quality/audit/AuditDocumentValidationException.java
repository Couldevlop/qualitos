package com.openlab.qualitos.quality.audit;

/** Fichier refusé : vide, type hors liste blanche, ou contenu qui ne correspond pas au type annoncé. */
public class AuditDocumentValidationException extends RuntimeException {
    public AuditDocumentValidationException(String message) { super(message); }
}
