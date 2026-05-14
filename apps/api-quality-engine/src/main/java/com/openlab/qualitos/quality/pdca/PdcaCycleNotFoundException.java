package com.openlab.qualitos.quality.pdca;

import java.util.UUID;

public class PdcaCycleNotFoundException extends RuntimeException {

    public PdcaCycleNotFoundException(UUID id) {
        super("PDCA cycle not found: " + id);
    }
}
