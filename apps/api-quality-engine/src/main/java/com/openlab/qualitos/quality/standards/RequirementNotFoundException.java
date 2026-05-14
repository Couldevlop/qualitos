package com.openlab.qualitos.quality.standards;

import java.util.UUID;

public class RequirementNotFoundException extends RuntimeException {
    public RequirementNotFoundException(UUID id) { super("Standard requirement not found: " + id); }
}
