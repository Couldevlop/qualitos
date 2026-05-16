package com.openlab.qualitos.quality.gdpr.domain;

import java.util.UUID;

public class SubjectRequestNotFoundException extends RuntimeException {
    public SubjectRequestNotFoundException(UUID id) {
        super("Data subject request not found: " + id);
    }
}
