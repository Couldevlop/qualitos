package com.openlab.qualitos.quality.change;

import java.util.UUID;

public class ChangeChildNotFoundException extends RuntimeException {
    public ChangeChildNotFoundException(String resource, UUID id) {
        super(resource + " not found: " + id);
    }
}
