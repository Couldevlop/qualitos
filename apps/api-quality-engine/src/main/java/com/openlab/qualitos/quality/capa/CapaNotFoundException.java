package com.openlab.qualitos.quality.capa;

import java.util.UUID;

public class CapaNotFoundException extends RuntimeException {
    public CapaNotFoundException(UUID id) { super("CAPA case not found: " + id); }
}
