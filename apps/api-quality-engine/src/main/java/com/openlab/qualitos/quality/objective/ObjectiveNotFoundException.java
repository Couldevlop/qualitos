package com.openlab.qualitos.quality.objective;

import java.util.UUID;

/** Objectif inconnu, ou appartenant a un autre client -- indistinguable, et c'est voulu. */
public class ObjectiveNotFoundException extends RuntimeException {

    public ObjectiveNotFoundException(UUID id) {
        super("Quality objective " + id + " not found");
    }
}
