package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class TenantStandardNotFoundException extends RuntimeException {
    public TenantStandardNotFoundException(UUID id) { super("Tenant standard adoption not found: " + id); }
}
