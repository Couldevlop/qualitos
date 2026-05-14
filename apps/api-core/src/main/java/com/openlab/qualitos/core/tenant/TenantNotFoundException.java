package com.openlab.qualitos.core.tenant;

import java.util.UUID;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(UUID id) {
        super("Tenant not found with id: " + id);
    }

    public TenantNotFoundException(String slug) {
        super("Tenant not found with slug: " + slug);
    }
}
