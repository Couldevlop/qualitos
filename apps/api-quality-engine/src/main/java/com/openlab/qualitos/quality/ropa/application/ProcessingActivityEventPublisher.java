package com.openlab.qualitos.quality.ropa.application;

import com.openlab.qualitos.quality.ropa.domain.ProcessingActivity;

public interface ProcessingActivityEventPublisher {

    enum Action { CREATED, EDITED, ACTIVATED, ARCHIVED, DELETED }

    void publish(ProcessingActivity activity, Action action);

    final class NoOp implements ProcessingActivityEventPublisher {
        @Override public void publish(ProcessingActivity a, Action act) { }
    }
}
