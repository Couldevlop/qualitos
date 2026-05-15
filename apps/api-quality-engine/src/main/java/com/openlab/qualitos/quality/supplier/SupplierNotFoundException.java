package com.openlab.qualitos.quality.supplier;

import java.util.UUID;

public class SupplierNotFoundException extends RuntimeException {
    public SupplierNotFoundException(UUID id) { super("Supplier not found: " + id); }
}
