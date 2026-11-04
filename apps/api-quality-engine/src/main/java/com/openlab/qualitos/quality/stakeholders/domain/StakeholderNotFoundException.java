package com.openlab.qualitos.quality.stakeholders.domain;

import java.util.UUID;

/** La fiche demandée n'existe pas, ou n'est pas celle de ce client : 404. */
public class StakeholderNotFoundException extends RuntimeException {

    public StakeholderNotFoundException(UUID id) {
        super("Stakeholder not found: " + id);
    }
}
