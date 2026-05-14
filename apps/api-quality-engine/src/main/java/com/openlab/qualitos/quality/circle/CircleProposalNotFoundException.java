package com.openlab.qualitos.quality.circle;

import java.util.UUID;

public class CircleProposalNotFoundException extends RuntimeException {
    public CircleProposalNotFoundException(UUID id) { super("Circle proposal not found: " + id); }
}
