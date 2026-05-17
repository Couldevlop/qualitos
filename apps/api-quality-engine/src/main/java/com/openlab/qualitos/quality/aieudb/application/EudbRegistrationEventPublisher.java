package com.openlab.qualitos.quality.aieudb.application;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;

public interface EudbRegistrationEventPublisher {

    enum Action { DRAFTED, EDITED, SUBMITTED, REGISTERED, UPDATED, REJECTED, RETIRED, DELETED }

    void publish(EudbRegistration registration, Action action);

    final class NoOp implements EudbRegistrationEventPublisher {
        @Override public void publish(EudbRegistration r, Action a) { }
    }
}
