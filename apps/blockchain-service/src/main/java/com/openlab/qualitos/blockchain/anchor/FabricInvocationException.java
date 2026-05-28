package com.openlab.qualitos.blockchain.anchor;

/** Échec d'invocation du chaincode {@code qualitos-anchor} (endorsement/submit/commit). */
public class FabricInvocationException extends RuntimeException {

    public FabricInvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FabricInvocationException(String message) {
        super(message);
    }
}
