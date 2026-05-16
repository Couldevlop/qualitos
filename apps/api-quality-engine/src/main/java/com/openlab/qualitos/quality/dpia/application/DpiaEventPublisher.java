package com.openlab.qualitos.quality.dpia.application;

import com.openlab.qualitos.quality.dpia.domain.Dpia;

public interface DpiaEventPublisher {

    enum Action {
        CREATED, EDITED, STARTED, RETURNED_TO_DRAFT,
        SUBMITTED_TO_DPO, APPROVED, REJECTED, ARCHIVED, DELETED
    }

    void publish(Dpia dpia, Action action);

    final class NoOp implements DpiaEventPublisher {
        @Override public void publish(Dpia d, Action a) { }
    }
}
