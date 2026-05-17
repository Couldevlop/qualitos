package com.openlab.qualitos.quality.aiqms.domain;

import java.util.UUID;

public class AiQmsNotFoundException extends RuntimeException {
    public AiQmsNotFoundException(UUID id) {
        super("AI QMS not found: " + id);
    }
}
