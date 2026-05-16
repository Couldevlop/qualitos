package com.openlab.qualitos.quality.automateddecisions.domain;

import java.util.UUID;

public class AutomatedDecisionNotFoundException extends RuntimeException {
    public AutomatedDecisionNotFoundException(UUID id) {
        super("Automated decision not found: " + id);
    }
}
