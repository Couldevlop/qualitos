package com.openlab.qualitos.quality.processoragreements.domain;

import java.util.UUID;

public class ProcessorAgreementNotFoundException extends RuntimeException {
    public ProcessorAgreementNotFoundException(UUID id) {
        super("Processor agreement not found: " + id);
    }
}
