package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class EvidenceNotFoundException extends RuntimeException {
    public EvidenceNotFoundException(UUID id) { super("Requirement evidence not found: " + id); }
}
