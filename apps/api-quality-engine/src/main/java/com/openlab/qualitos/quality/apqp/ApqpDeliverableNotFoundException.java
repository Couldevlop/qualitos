package com.openlab.qualitos.quality.apqp;

import java.util.UUID;

public class ApqpDeliverableNotFoundException extends RuntimeException {
    public ApqpDeliverableNotFoundException(UUID id) { super("APQP deliverable not found: " + id); }
}
