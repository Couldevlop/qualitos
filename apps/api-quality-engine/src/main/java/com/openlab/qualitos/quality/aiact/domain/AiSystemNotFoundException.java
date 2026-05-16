package com.openlab.qualitos.quality.aiact.domain;

import java.util.UUID;

public class AiSystemNotFoundException extends RuntimeException {
    public AiSystemNotFoundException(UUID id) {
        super("AI system not found: " + id);
    }
}
