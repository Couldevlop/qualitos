package com.openlab.qualitos.quality.dpoappointments.domain;

import java.util.UUID;

public class DpoAppointmentNotFoundException extends RuntimeException {
    public DpoAppointmentNotFoundException(UUID id) {
        super("DPO appointment not found: " + id);
    }
}
