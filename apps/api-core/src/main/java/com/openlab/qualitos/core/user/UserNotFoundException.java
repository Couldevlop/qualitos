package com.openlab.qualitos.core.user;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("User not found with id: " + id);
    }

    public UserNotFoundException(String keycloakId) {
        super("User not found with keycloakId: " + keycloakId);
    }
}
