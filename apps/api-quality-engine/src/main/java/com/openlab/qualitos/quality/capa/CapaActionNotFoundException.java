package com.openlab.qualitos.quality.capa;

import java.util.UUID;

public class CapaActionNotFoundException extends RuntimeException {
    public CapaActionNotFoundException(UUID id) { super("CAPA action not found: " + id); }
}
