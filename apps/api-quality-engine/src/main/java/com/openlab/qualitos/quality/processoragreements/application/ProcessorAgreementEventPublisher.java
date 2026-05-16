package com.openlab.qualitos.quality.processoragreements.application;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;

public interface ProcessorAgreementEventPublisher {

    enum Action { CREATED, EDITED, ACTIVATED, TERMINATED, EXPIRED, DELETED }

    void publish(ProcessorAgreement agreement, Action action);

    final class NoOp implements ProcessorAgreementEventPublisher {
        @Override public void publish(ProcessorAgreement a, Action act) { }
    }
}
