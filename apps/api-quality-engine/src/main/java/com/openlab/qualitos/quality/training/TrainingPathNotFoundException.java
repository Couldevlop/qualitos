package com.openlab.qualitos.quality.training;

import java.util.UUID;

public class TrainingPathNotFoundException extends RuntimeException {
    public TrainingPathNotFoundException(UUID id) { super("Training path not found: " + id); }
}
