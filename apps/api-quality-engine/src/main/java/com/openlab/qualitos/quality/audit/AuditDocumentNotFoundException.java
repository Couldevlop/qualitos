package com.openlab.qualitos.quality.audit;

import java.util.UUID;

public class AuditDocumentNotFoundException extends RuntimeException {
    public AuditDocumentNotFoundException(UUID id) { super("Audit document not found: " + id); }
}
