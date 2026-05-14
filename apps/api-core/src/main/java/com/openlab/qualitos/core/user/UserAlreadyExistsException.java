package com.openlab.qualitos.core.user;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String keycloakId) {
        super("User with keycloakId '" + keycloakId + "' already exists");
    }
}
