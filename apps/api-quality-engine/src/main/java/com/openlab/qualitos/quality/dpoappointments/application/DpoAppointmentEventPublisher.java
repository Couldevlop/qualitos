package com.openlab.qualitos.quality.dpoappointments.application;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointment;

public interface DpoAppointmentEventPublisher {

    enum Action { PROPOSED, EDITED, ACTIVATED, ENDED, CANCELLED, DELETED }

    void publish(DpoAppointment appointment, Action action);

    final class NoOp implements DpoAppointmentEventPublisher {
        @Override public void publish(DpoAppointment a, Action action) { }
    }
}
