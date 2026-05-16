package com.openlab.qualitos.quality.consent.domain;

import java.util.UUID;

public class ConsentNotFoundException extends RuntimeException {
    public ConsentNotFoundException(UUID id) {
        super("Consent not found: " + id);
    }
}
