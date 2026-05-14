package com.openlab.qualitos.core.tenant;

public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String slug) {
        super("Tenant with slug '" + slug + "' already exists");
    }
}
