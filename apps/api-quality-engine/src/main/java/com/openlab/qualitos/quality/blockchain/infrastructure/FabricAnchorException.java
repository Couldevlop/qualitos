package com.openlab.qualitos.quality.blockchain.infrastructure;

/**
 * Échec d'un appel d'ancrage vers {@code blockchain-service} (réseau Hyperledger
 * Fabric indisponible, timeout, réponse invalide…). Déclenche le repli sur la
 * notarisation signée Phase A (ADR 0012).
 */
public class FabricAnchorException extends RuntimeException {

    public FabricAnchorException(String message) {
        super(message);
    }

    public FabricAnchorException(String message, Throwable cause) {
        super(message, cause);
    }
}
