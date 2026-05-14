package com.openlab.qualitos.quality.audit;

import java.util.UUID;

public class AuditPlanNotFoundException extends RuntimeException {
    public AuditPlanNotFoundException(UUID id) { super("Audit plan not found: " + id); }
}
