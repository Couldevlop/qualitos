package com.openlab.qualitos.quality.training;

import java.util.UUID;

public class EnrollmentNotFoundException extends RuntimeException {
    public EnrollmentNotFoundException(UUID id) { super("Enrollment not found: " + id); }
    public EnrollmentNotFoundException(String certificateCode) {
        super("Certificate not found: " + certificateCode);
    }
}
