package com.openlab.qualitos.quality.apqp;

import java.util.UUID;

public class ApqpPhaseNotFoundException extends RuntimeException {
    public ApqpPhaseNotFoundException(UUID id) { super("APQP phase not found: " + id); }
}
