package com.openlab.qualitos.quality.capa;

/**
 * Pièce dépassant la taille maximale autorisée (OWASP — limitation des
 * ressources). Mappée en 413 Payload Too Large.
 */
public class CapaEvidenceTooLargeException extends RuntimeException {
    public CapaEvidenceTooLargeException(long sizeBytes, long maxBytes) {
        super("Evidence size " + sizeBytes + " bytes exceeds the maximum of " + maxBytes + " bytes");
    }
}
