package com.openlab.qualitos.quality.tenantmodules.application;

import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivation;

/**
 * Port — chaque transition publie un événement (audit log, bus, etc.).
 * NoOp par défaut côté tests.
 */
public interface ModuleActivationEventPublisher {

    enum Action {
        TRIAL_STARTED, ACTIVATED, SUSPENDED, RESUMED,
        TIER_CHANGED, CONFIGURED, EXPIRED, DISABLED
    }

    void publish(ModuleActivation activation, Action action);

    final class NoOp implements ModuleActivationEventPublisher {
        @Override public void publish(ModuleActivation a, Action action) { }
    }
}
