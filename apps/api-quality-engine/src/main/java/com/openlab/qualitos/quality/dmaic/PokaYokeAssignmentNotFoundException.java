package com.openlab.qualitos.quality.dmaic;

import java.util.UUID;

public class PokaYokeAssignmentNotFoundException extends RuntimeException {
    public PokaYokeAssignmentNotFoundException(UUID id) { super("Poka-Yoke assignment not found: " + id); }
}
