package com.openlab.qualitos.quality.fives;

import java.util.UUID;

public class FiveSAuditNotFoundException extends RuntimeException {
    public FiveSAuditNotFoundException(UUID id) {
        super("5S audit not found: " + id);
    }
}
