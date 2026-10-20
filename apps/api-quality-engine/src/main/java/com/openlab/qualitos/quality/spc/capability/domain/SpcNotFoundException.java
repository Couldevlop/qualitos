package com.openlab.qualitos.quality.spc.capability.domain;

import java.util.UUID;

/** L'étude n'existe pas — ou pas dans ce client, ce qui revient au même ici. */
public class SpcNotFoundException extends RuntimeException {

    public SpcNotFoundException(UUID id) {
        super("SPC study not found: " + id);
    }
}
