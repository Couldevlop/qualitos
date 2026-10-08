package com.openlab.qualitos.core.identity;

/** Le fournisseur d'identité a refusé, ou n'est pas joignable, ou n'est pas configuré (502). */
public class IdentityProviderException extends RuntimeException {

    public IdentityProviderException(String message) {
        super(message);
    }

    public IdentityProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
