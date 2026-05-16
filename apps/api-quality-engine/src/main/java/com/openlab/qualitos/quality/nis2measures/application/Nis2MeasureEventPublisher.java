package com.openlab.qualitos.quality.nis2measures.application;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;

public interface Nis2MeasureEventPublisher {

    enum Action {
        PLANNED, EDITED, STARTED, IMPLEMENTED, VERIFIED, REVIEWED, DEPRECATED, DELETED
    }

    void publish(Nis2RiskMeasure measure, Action action);

    final class NoOp implements Nis2MeasureEventPublisher {
        @Override public void publish(Nis2RiskMeasure m, Action a) { }
    }
}
