package com.openlab.qualitos.quality.dmaic;

import java.util.UUID;

public class ProcessMeasureNotFoundException extends RuntimeException {
    public ProcessMeasureNotFoundException(UUID id) { super("Process measure not found: " + id); }
}
