package com.openlab.qualitos.quality.pdca;

/**
 * Pièce dépassant la taille maximale autorisée (OWASP — limitation des
 * ressources). Mappée en 413 Payload Too Large.
 */
public class PdcaStepEvidenceTooLargeException extends RuntimeException {
    public PdcaStepEvidenceTooLargeException(long sizeBytes, long maxBytes) {
        super("Evidence size " + sizeBytes + " bytes exceeds the maximum of " + maxBytes + " bytes");
    }
}
