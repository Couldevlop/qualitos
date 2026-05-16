package com.openlab.qualitos.quality.breach.domain;

import java.util.UUID;

public class BreachNotFoundException extends RuntimeException {
    public BreachNotFoundException(UUID id) {
        super("Breach incident not found: " + id);
    }
}
