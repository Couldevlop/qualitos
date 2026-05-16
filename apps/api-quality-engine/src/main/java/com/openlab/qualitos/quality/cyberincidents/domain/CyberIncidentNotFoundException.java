package com.openlab.qualitos.quality.cyberincidents.domain;

import java.util.UUID;

public class CyberIncidentNotFoundException extends RuntimeException {
    public CyberIncidentNotFoundException(UUID id) {
        super("Cyber incident not found: " + id);
    }
}
