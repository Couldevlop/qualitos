package com.openlab.qualitos.quality.controlplan.domain;

import java.util.UUID;

/** 404. Un plan d'un autre tenant lève la même chose : un 403 confirmerait son existence. */
public class ControlPlanNotFoundException extends RuntimeException {

    public ControlPlanNotFoundException(UUID id) {
        super("Control plan not found: " + id);
    }
}
