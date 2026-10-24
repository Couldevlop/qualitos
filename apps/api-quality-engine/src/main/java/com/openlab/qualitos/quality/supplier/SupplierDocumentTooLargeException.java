package com.openlab.qualitos.quality.supplier;

/** Plafond dépassé : taille d'un fichier, nombre de documents ou poids cumulé du porteur. */
public class SupplierDocumentTooLargeException extends RuntimeException {
    public SupplierDocumentTooLargeException(String message) { super(message); }
}
