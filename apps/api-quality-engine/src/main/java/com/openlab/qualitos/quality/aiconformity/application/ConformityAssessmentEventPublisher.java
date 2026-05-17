package com.openlab.qualitos.quality.aiconformity.application;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;

public interface ConformityAssessmentEventPublisher {

    enum Action { PLANNED, EDITED, STARTED, CERTIFIED, EXPIRED, REVOKED, FAILED, DELETED }

    void publish(ConformityAssessment assessment, Action action);

    final class NoOp implements ConformityAssessmentEventPublisher {
        @Override public void publish(ConformityAssessment a, Action act) { }
    }
}
