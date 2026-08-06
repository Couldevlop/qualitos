package com.openlab.qualitos.quality.fivewhys;

import java.util.UUID;

public class FiveWhysNotFoundException extends RuntimeException {
    public FiveWhysNotFoundException(UUID id) {
        super("Five-whys resource not found: " + id);
    }
}
