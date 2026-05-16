package com.openlab.qualitos.quality.aiactfria.application;

import com.openlab.qualitos.quality.aiactfria.domain.Fria;

public interface FriaEventPublisher {

    enum Action { DRAFTED, EDITED, SUBMITTED, RETURNED_TO_DRAFT, APPROVED, ARCHIVED, DELETED }

    void publish(Fria fria, Action action);

    final class NoOp implements FriaEventPublisher {
        @Override public void publish(Fria f, Action a) { }
    }
}
