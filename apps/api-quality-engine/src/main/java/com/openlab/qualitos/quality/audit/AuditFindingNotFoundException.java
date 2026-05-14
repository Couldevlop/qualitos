package com.openlab.qualitos.quality.audit;

import java.util.UUID;

public class AuditFindingNotFoundException extends RuntimeException {
    public AuditFindingNotFoundException(UUID id) {
        super("Audit finding not found: " + id);
    }
}
