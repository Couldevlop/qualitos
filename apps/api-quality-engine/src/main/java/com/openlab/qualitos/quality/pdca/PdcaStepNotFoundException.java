package com.openlab.qualitos.quality.pdca;

import java.util.UUID;

public class PdcaStepNotFoundException extends RuntimeException {

    public PdcaStepNotFoundException(UUID stepId) {
        super("PDCA step not found: " + stepId);
    }
}
