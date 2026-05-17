package com.openlab.qualitos.quality.aiconformity.domain;

import java.util.UUID;

public class ConformityAssessmentNotFoundException extends RuntimeException {
    public ConformityAssessmentNotFoundException(UUID id) {
        super("Conformity assessment not found: " + id);
    }
}
