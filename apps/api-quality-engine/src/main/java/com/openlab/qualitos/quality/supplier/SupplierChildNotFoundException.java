package com.openlab.qualitos.quality.supplier;

import java.util.UUID;

public class SupplierChildNotFoundException extends RuntimeException {
    public SupplierChildNotFoundException(String resource, UUID id) {
        super(resource + " not found: " + id);
    }
}
