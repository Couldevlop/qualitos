package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class ClauseNotFoundException extends RuntimeException {
    public ClauseNotFoundException(UUID id) { super("Standard clause not found: " + id); }
}
