package com.openlab.qualitos.quality.dmaic;

import java.util.UUID;

public class PokaYokeDeviceNotFoundException extends RuntimeException {
    public PokaYokeDeviceNotFoundException(UUID id) { super("Poka-Yoke device not found: " + id); }
    public PokaYokeDeviceNotFoundException(String code) { super("Poka-Yoke device not found: " + code); }
}
