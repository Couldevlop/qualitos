package com.openlab.qualitos.quality.itsm;

import java.util.UUID;

public class ItsmConnectionNotFoundException extends RuntimeException {
    public ItsmConnectionNotFoundException(UUID id) {
        super("ITSM connection not found: " + id);
    }
}
