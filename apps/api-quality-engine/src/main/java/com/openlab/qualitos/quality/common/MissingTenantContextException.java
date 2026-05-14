package com.openlab.qualitos.quality.common;

public class MissingTenantContextException extends RuntimeException {

    public MissingTenantContextException() {
        super("No tenant_id found in JWT — request requires a tenant context");
    }
}
