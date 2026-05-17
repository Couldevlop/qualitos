package com.openlab.qualitos.quality.aiqms.application;

import com.openlab.qualitos.quality.aiqms.domain.AiQms;

public interface AiQmsEventPublisher {

    enum Action { DRAFTED, EDITED, APPROVED, IN_FORCE, SUPERSEDED, ARCHIVED, DELETED }

    void publish(AiQms qms, Action action);

    final class NoOp implements AiQmsEventPublisher {
        @Override public void publish(AiQms q, Action a) { }
    }
}
