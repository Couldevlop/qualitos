package com.openlab.qualitos.quality.pdca;

import java.util.UUID;

/** Pièce de preuve absente de l'étape, ou d'un autre tenant. Mappée en 404. */
public class PdcaStepEvidenceNotFoundException extends RuntimeException {
    public PdcaStepEvidenceNotFoundException(UUID id) {
        super("PDCA step evidence not found: " + id);
    }
}
