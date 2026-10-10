package com.openlab.qualitos.core.identity;

/** Un rôle demandé n'est pas de ceux qu'un client peut attribuer (422). */
public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(String message) {
        super(message);
    }
}
