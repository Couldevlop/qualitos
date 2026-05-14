package com.openlab.qualitos.quality.circle;

import java.util.UUID;

public class CircleNotFoundException extends RuntimeException {
    public CircleNotFoundException(UUID id) { super("Quality circle not found: " + id); }
}
