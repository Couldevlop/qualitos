package com.openlab.qualitos.quality.ehs.domain;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {
    public IncidentNotFoundException(UUID id) { super("EHS incident not found: " + id); }
}
