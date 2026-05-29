package com.openlab.qualitos.quality.iot;

import java.util.UUID;

public class IotThresholdNotFoundException extends RuntimeException {
    public IotThresholdNotFoundException(UUID id) { super("IoT threshold not found: " + id); }
}
