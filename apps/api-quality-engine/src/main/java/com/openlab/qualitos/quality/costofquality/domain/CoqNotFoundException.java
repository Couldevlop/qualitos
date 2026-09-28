package com.openlab.qualitos.quality.costofquality.domain;

import java.util.UUID;

/** La ligne ou le libellé n'existe pas — ou pas dans ce client, ce qui revient au même ici. */
public class CoqNotFoundException extends RuntimeException {

    public CoqNotFoundException(String what, UUID id) {
        super(what + " not found: " + id);
    }
}
