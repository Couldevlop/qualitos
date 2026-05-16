package com.openlab.qualitos.quality.aiactfria.domain;

import java.util.UUID;

public class FriaNotFoundException extends RuntimeException {
    public FriaNotFoundException(UUID id) {
        super("FRIA not found: " + id);
    }
}
