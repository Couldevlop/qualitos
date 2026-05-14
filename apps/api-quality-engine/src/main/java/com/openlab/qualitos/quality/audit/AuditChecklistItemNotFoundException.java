package com.openlab.qualitos.quality.audit;

import java.util.UUID;

public class AuditChecklistItemNotFoundException extends RuntimeException {
    public AuditChecklistItemNotFoundException(UUID id) {
        super("Audit checklist item not found: " + id);
    }
}
