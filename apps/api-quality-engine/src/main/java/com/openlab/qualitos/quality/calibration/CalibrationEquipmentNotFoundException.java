package com.openlab.qualitos.quality.calibration;

import java.util.UUID;

public class CalibrationEquipmentNotFoundException extends RuntimeException {
    public CalibrationEquipmentNotFoundException(UUID id) {
        super("Calibration equipment not found: " + id);
    }
}
