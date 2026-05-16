package com.openlab.qualitos.quality.aiincidents.application;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;

public interface AiIncidentEventPublisher {

    enum Action {
        DETECTED, EDITED, INVESTIGATION_STARTED, NOTIFIED_REGULATOR,
        CLOSED, DISMISSED, DELETED
    }

    void publish(AiIncident incident, Action action);

    final class NoOp implements AiIncidentEventPublisher {
        @Override public void publish(AiIncident i, Action a) { }
    }
}
