package com.openlab.qualitos.quality.controlplan.domain;

/** 409. Une écriture sur un document qui n'est plus un brouillon. */
public class ControlPlanStateException extends RuntimeException {

    public ControlPlanStateException(String message) {
        super(message);
    }
}
