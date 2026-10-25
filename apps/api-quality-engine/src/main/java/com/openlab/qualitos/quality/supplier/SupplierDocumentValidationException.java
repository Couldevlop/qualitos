package com.openlab.qualitos.quality.supplier;

/** Fichier refusé : vide, type hors liste blanche, ou contenu qui ne correspond pas au type annoncé. */
public class SupplierDocumentValidationException extends RuntimeException {
    public SupplierDocumentValidationException(String message) { super(message); }
}
