package com.openlab.qualitos.quality.aipmm.application;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;

public interface PmmPlanEventPublisher {

    enum Action { DRAFTED, EDITED, ACTIVATED, REVIEWED, SUSPENDED, CLOSED, DELETED }

    void publish(PmmPlan plan, Action action);

    final class NoOp implements PmmPlanEventPublisher {
        @Override public void publish(PmmPlan p, Action a) { }
    }
}
