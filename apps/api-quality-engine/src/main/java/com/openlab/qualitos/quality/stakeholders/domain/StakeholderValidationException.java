package com.openlab.qualitos.quality.stakeholders.domain;

/** Une saisie que le domaine refuse : 400. */
public class StakeholderValidationException extends RuntimeException {

    public StakeholderValidationException(String message) {
        super(message);
    }
}
