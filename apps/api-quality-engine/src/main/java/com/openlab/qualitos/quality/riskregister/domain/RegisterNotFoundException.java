package com.openlab.qualitos.quality.riskregister.domain;

import java.util.UUID;

/** La fiche n'existe pas — ou pas dans ce client, ce qui revient au même ici. */
public class RegisterNotFoundException extends RuntimeException {

    public RegisterNotFoundException(String what, UUID id) {
        super(what + " not found: " + id);
    }
}
