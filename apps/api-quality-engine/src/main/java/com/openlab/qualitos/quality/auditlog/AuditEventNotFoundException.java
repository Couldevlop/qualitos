package com.openlab.qualitos.quality.auditlog;

import java.util.UUID;

public class AuditEventNotFoundException extends RuntimeException {
    public AuditEventNotFoundException(UUID id) { super("Audit event not found: " + id); }
}
