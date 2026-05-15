package com.openlab.qualitos.quality.calibration;

import java.util.UUID;

public class CalibrationChildNotFoundException extends RuntimeException {
    public CalibrationChildNotFoundException(String resource, UUID id) {
        super(resource + " not found: " + id);
    }
}
