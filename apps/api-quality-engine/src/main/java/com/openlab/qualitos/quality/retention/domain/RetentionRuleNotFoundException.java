package com.openlab.qualitos.quality.retention.domain;

import java.util.UUID;

public class RetentionRuleNotFoundException extends RuntimeException {
    public RetentionRuleNotFoundException(UUID id) {
        super("Retention rule not found: " + id);
    }
}
