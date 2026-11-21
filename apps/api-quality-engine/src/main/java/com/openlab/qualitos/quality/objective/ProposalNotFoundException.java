package com.openlab.qualitos.quality.objective;

import java.util.UUID;

/** Proposition inconnue, ou appartenant a un autre client. */
public class ProposalNotFoundException extends RuntimeException {

    public ProposalNotFoundException(UUID id) {
        super("Quality objective proposal " + id + " not found");
    }
}
