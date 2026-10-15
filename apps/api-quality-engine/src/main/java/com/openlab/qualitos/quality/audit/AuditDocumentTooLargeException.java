package com.openlab.qualitos.quality.audit;

/** Plafond dépassé : taille d'un fichier, nombre de documents ou poids cumulé de l'audit. */
public class AuditDocumentTooLargeException extends RuntimeException {
    public AuditDocumentTooLargeException(String message) { super(message); }
}
