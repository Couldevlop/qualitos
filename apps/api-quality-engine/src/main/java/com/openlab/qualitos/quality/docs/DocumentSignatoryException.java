package com.openlab.qualitos.quality.docs;

/**
 * Une liste de signataires que la soumission ne peut pas accepter : vide, trop
 * longue, avec un doublon ou l'auteur lui-même (ADR 0099). Répond 400.
 */
public class DocumentSignatoryException extends RuntimeException {
    public DocumentSignatoryException(String message) { super(message); }
}
