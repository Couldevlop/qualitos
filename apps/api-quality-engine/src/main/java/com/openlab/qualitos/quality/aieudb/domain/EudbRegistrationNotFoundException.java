package com.openlab.qualitos.quality.aieudb.domain;

import java.util.UUID;

public class EudbRegistrationNotFoundException extends RuntimeException {
    public EudbRegistrationNotFoundException(UUID id) {
        super("EUDB registration not found: " + id);
    }
}
