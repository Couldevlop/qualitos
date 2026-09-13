package com.openlab.qualitos.quality.nonconformity.eightd.domain;

import java.util.UUID;

/** Aucun rapport 8D pour cette non-conformité dans ce tenant. Rendu en 404. */
public class EightDReportNotFoundException extends RuntimeException {

    public EightDReportNotFoundException(UUID ncId) {
        super("Aucun rapport 8D pour la non-conformite " + ncId);
    }
}
