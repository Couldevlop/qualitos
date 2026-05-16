package com.openlab.qualitos.quality.retention.application;

import com.openlab.qualitos.quality.retention.domain.RetentionRule;

public interface RetentionRuleEventPublisher {

    enum Action { CREATED, EDITED, ACTIVATED, ARCHIVED, DELETED }

    void publish(RetentionRule rule, Action action);

    final class NoOp implements RetentionRuleEventPublisher {
        @Override public void publish(RetentionRule r, Action a) { }
    }
}
