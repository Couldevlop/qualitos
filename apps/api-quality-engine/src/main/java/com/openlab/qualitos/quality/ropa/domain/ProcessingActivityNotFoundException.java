package com.openlab.qualitos.quality.ropa.domain;

import java.util.UUID;

public class ProcessingActivityNotFoundException extends RuntimeException {
    public ProcessingActivityNotFoundException(UUID id) {
        super("Processing activity not found: " + id);
    }
}
