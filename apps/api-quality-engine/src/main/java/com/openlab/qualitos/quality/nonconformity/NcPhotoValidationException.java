package com.openlab.qualitos.quality.nonconformity;

/**
 * Photo invalide à l'upload : fichier vide ou content-type hors whitelist
 * (OWASP — validation stricte des entrées). Mappée en 400 Bad Request.
 */
public class NcPhotoValidationException extends RuntimeException {
    public NcPhotoValidationException(String message) { super(message); }
}
