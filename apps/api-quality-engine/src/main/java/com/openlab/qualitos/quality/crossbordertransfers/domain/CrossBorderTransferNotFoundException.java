package com.openlab.qualitos.quality.crossbordertransfers.domain;

import java.util.UUID;

public class CrossBorderTransferNotFoundException extends RuntimeException {
    public CrossBorderTransferNotFoundException(UUID id) {
        super("Cross-border transfer not found: " + id);
    }
}
