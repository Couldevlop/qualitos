package com.openlab.qualitos.quality.gdpr.application;

import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequest;

public interface SubjectRequestEventPublisher {

    enum Action { RECEIVED, IN_PROGRESS, COMPLETED, REJECTED, EXTENDED }

    void publish(DataSubjectRequest request, Action action);

    final class NoOp implements SubjectRequestEventPublisher {
        @Override public void publish(DataSubjectRequest r, Action a) { }
    }
}
