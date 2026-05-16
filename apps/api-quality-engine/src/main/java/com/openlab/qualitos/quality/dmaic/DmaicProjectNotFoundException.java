package com.openlab.qualitos.quality.dmaic;

import java.util.UUID;

public class DmaicProjectNotFoundException extends RuntimeException {
    public DmaicProjectNotFoundException(UUID id) { super("DMAIC project not found: " + id); }
}
