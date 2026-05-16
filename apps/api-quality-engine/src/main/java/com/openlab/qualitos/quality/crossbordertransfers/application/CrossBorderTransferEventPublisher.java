package com.openlab.qualitos.quality.crossbordertransfers.application;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;

public interface CrossBorderTransferEventPublisher {

    enum Action { CREATED, EDITED, ACTIVATED, SUSPENDED, REACTIVATED, TERMINATED, DELETED }

    void publish(CrossBorderTransfer transfer, Action action);

    final class NoOp implements CrossBorderTransferEventPublisher {
        @Override public void publish(CrossBorderTransfer t, Action a) { }
    }
}
