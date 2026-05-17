package com.openlab.qualitos.quality.aipmm.domain;

import java.util.UUID;

public class PmmPlanNotFoundException extends RuntimeException {
    public PmmPlanNotFoundException(UUID id) {
        super("PMM plan not found: " + id);
    }
}
