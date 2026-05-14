package com.openlab.qualitos.quality.circle;

import java.util.UUID;

public class CircleMemberNotFoundException extends RuntimeException {
    public CircleMemberNotFoundException(UUID id) { super("Circle member not found: " + id); }
}
