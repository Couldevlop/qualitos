package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class SectionNotFoundException extends RuntimeException {
    public SectionNotFoundException(UUID id) { super("Standard section not found: " + id); }
}
