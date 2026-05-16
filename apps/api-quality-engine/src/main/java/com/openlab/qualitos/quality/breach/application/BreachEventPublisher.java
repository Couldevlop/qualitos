package com.openlab.qualitos.quality.breach.application;

import com.openlab.qualitos.quality.breach.domain.BreachIncident;

public interface BreachEventPublisher {

    enum Action {
        DETECTED, ASSESSING, CONTAINED, DPA_NOTIFIED,
        SUBJECTS_NOTIFIED, CLOSED, REJECTED, SEVERITY_UPDATED
    }

    void publish(BreachIncident incident, Action action);

    final class NoOp implements BreachEventPublisher {
        @Override public void publish(BreachIncident i, Action a) { }
    }
}
