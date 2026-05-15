package com.openlab.qualitos.quality.risk;

import java.util.UUID;

public class FmeaProjectNotFoundException extends RuntimeException {
    public FmeaProjectNotFoundException(UUID id) { super("FMEA project not found: " + id); }
    public FmeaProjectNotFoundException(String code) { super("FMEA project not found: " + code); }
}
