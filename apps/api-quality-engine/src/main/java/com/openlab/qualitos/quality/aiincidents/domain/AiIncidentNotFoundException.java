package com.openlab.qualitos.quality.aiincidents.domain;

import java.util.UUID;

public class AiIncidentNotFoundException extends RuntimeException {
    public AiIncidentNotFoundException(UUID id) {
        super("AI incident not found: " + id);
    }
}
