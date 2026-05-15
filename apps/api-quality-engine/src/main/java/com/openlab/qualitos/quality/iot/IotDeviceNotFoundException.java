package com.openlab.qualitos.quality.iot;

import java.util.UUID;

public class IotDeviceNotFoundException extends RuntimeException {
    public IotDeviceNotFoundException(UUID id) { super("IoT device not found: " + id); }
    public IotDeviceNotFoundException(String code) { super("IoT device not found: " + code); }
}
