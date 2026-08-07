package com.openlab.qualitos.quality.capa;

/**
 * Pièce refusée à l'entrée : fichier vide, type hors liste blanche, ou signature
 * binaire incohérente avec le type déclaré (OWASP — validation stricte des
 * entrées). Mappée en 400 Bad Request.
 */
public class CapaEvidenceValidationException extends RuntimeException {
    public CapaEvidenceValidationException(String message) { super(message); }
}
