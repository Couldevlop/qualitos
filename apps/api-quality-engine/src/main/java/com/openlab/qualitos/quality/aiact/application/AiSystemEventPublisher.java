package com.openlab.qualitos.quality.aiact.application;

import com.openlab.qualitos.quality.aiact.domain.AiSystem;

public interface AiSystemEventPublisher {

    enum Action { DRAFTED, EDITED, REGISTERED, PUT_IN_USE, DECOMMISSIONED, WITHDRAWN, DELETED }

    void publish(AiSystem system, Action action);

    final class NoOp implements AiSystemEventPublisher {
        @Override public void publish(AiSystem s, Action a) { }
    }
}
