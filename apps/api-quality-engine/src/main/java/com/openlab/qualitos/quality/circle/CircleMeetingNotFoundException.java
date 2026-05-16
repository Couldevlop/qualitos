package com.openlab.qualitos.quality.circle;

import java.util.UUID;

public class CircleMeetingNotFoundException extends RuntimeException {
    public CircleMeetingNotFoundException(UUID id) { super("Circle meeting not found: " + id); }
}
