package com.openlab.qualitos.quality.risk;

import java.util.UUID;

public class FmeaItemNotFoundException extends RuntimeException {
    public FmeaItemNotFoundException(UUID id) { super("FMEA item not found: " + id); }
}
