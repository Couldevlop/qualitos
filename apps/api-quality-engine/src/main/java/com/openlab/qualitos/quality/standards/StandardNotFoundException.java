package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class StandardNotFoundException extends RuntimeException {
    public StandardNotFoundException(UUID id) { super("Standard not found: " + id); }
    public StandardNotFoundException(String code) { super("Standard not found: " + code); }
}
