package com.openlab.qualitos.quality.cyberincidents.application;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;

public interface CyberIncidentEventPublisher {

    enum Action {
        DETECTED, ASSESSING, MITIGATED, CLOSED, REJECTED,
        EARLY_WARNING_SENT, INITIAL_ASSESSMENT_SENT, FINAL_REPORT_SENT,
        SEVERITY_UPDATED, BREACH_LINKED
    }

    void publish(CyberIncident incident, Action action);

    final class NoOp implements CyberIncidentEventPublisher {
        @Override public void publish(CyberIncident i, Action a) { }
    }
}
