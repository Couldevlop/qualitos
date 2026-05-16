package com.openlab.qualitos.quality.automateddecisions.application;

import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;

public interface AutomatedDecisionEventPublisher {

    enum Action { CREATED, EDITED, ACTIVATED, DEPRECATED, ARCHIVED, DELETED }

    void publish(AutomatedDecisionRecord record, Action action);

    final class NoOp implements AutomatedDecisionEventPublisher {
        @Override public void publish(AutomatedDecisionRecord r, Action a) { }
    }
}
