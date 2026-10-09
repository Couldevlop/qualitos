package com.openlab.qualitos.quality.authz.domain;

/** Un rôle qui n'existe pas dans le client du jeton (404, sans dire s'il existe ailleurs). */
public class AuthzNotFoundException extends RuntimeException {

    public AuthzNotFoundException(String message) {
        super(message);
    }
}
