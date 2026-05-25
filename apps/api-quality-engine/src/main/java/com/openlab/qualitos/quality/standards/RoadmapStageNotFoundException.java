package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class RoadmapStageNotFoundException extends RuntimeException {
    public RoadmapStageNotFoundException(UUID id) {
        super("Certification roadmap stage not found: " + id);
    }
}
