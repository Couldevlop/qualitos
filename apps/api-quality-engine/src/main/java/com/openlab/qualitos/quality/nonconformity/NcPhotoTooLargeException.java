package com.openlab.qualitos.quality.nonconformity;

/**
 * Photo dépassant la taille maximale autorisée (OWASP — limitation des
 * ressources). Mappée en 413 Payload Too Large.
 */
public class NcPhotoTooLargeException extends RuntimeException {
    public NcPhotoTooLargeException(long sizeBytes, long maxBytes) {
        super("Photo size " + sizeBytes + " bytes exceeds the maximum of " + maxBytes + " bytes");
    }
}
