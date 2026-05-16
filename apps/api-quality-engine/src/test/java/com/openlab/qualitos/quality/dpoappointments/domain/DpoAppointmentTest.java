package com.openlab.qualitos.quality.dpoappointments.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DpoAppointmentTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void propose_validInputs_createsProposed() {
        DpoAppointment a = proposeInternal();
        assertThat(a.isProposed()).isTrue();
        assertThat(a.getDpoEmail()).isEqualTo("dpo@example.com");
    }

    @Test
    void propose_invalidReference_throws() {
        assertThatThrownBy(() -> DpoAppointment.propose(T, "lowercase",
                "Jane Doe", "dpo@example.com", null,
                DpoType.INTERNAL, null, null, "GROUP", Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void propose_invalidEmail_throws() {
        assertThatThrownBy(() -> DpoAppointment.propose(T, "DPO-1",
                "Jane", "not-an-email", null,
                DpoType.INTERNAL, null, null, "GROUP", Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void propose_invalidScope_throws() {
        assertThatThrownBy(() -> DpoAppointment.propose(T, "DPO-1",
                "Jane", "dpo@x.com", null,
                DpoType.INTERNAL, null, null, "lowercase-bad", Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void propose_externalWithoutCompany_throws() {
        assertThatThrownBy(() -> DpoAppointment.propose(T, "DPO-1",
                "Jane", "dpo@x.com", null,
                DpoType.EXTERNAL, null, null, "GROUP", Set.of(), U, NOW))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void propose_externalWithCompany_ok() {
        DpoAppointment a = DpoAppointment.propose(T, "DPO-1",
                "Jane", "dpo@x.com", null,
                DpoType.EXTERNAL, "DPO Services Inc.", null,
                "GROUP", Set.of(), U, NOW);
        assertThat(a.getExternalCompanyName()).isEqualTo("DPO Services Inc.");
    }

    @Test
    void editProposed_changesFields() {
        DpoAppointment a = proposeInternal();
        a.editProposed("New Name", "new@x.com", "+33...",
                DpoType.INTERNAL, null, "ISO 27701 expert",
                Set.of(UUID.randomUUID()), NOW.plusSeconds(60));
        assertThat(a.getDpoFullName()).isEqualTo("New Name");
        assertThat(a.getDpoEmail()).isEqualTo("new@x.com");
    }

    @Test
    void editProposed_whenActive_rejected() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW, "CNIL-2026-001", NOW);
        assertThatThrownBy(() -> a.editProposed("X", "x@x.com", null,
                DpoType.INTERNAL, null, null, Set.of(), NOW.plusSeconds(60)))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void activate_requiresEffectiveFrom() {
        DpoAppointment a = proposeInternal();
        assertThatThrownBy(() -> a.activate(null, NOW, "ref", NOW))
                .isInstanceOf(DpoAppointmentStateException.class)
                .hasMessageContaining("effectiveFrom");
    }

    @Test
    void activate_requiresRegulatorNotification() {
        DpoAppointment a = proposeInternal();
        assertThatThrownBy(() -> a.activate(NOW, null, "ref", NOW))
                .isInstanceOf(DpoAppointmentStateException.class)
                .hasMessageContaining("Art. 37.7");
    }

    @Test
    void activate_requiresRegulatorReference() {
        DpoAppointment a = proposeInternal();
        assertThatThrownBy(() -> a.activate(NOW, NOW, " ", NOW))
                .isInstanceOf(DpoAppointmentStateException.class)
                .hasMessageContaining("regulatorNotificationReference");
    }

    @Test
    void activate_succeeds() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW.minusSeconds(86400), "CNIL-2026-001", NOW.plusSeconds(60));
        assertThat(a.isActive()).isTrue();
        assertThat(a.getEffectiveFrom()).isEqualTo(NOW);
        assertThat(a.getRegulatorNotificationReference()).isEqualTo("CNIL-2026-001");
    }

    @Test
    void activate_fromEnded_rejected() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW, "ref", NOW);
        a.end("Fin mandat", NOW.plusSeconds(60), NOW.plusSeconds(60));
        assertThatThrownBy(() -> a.activate(NOW.plusSeconds(120), NOW, "ref", NOW.plusSeconds(120)))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void end_succeeds() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW, "ref", NOW);
        a.end("Fin mandat", NOW.plusSeconds(86400), NOW.plusSeconds(86400));
        assertThat(a.isTerminal()).isTrue();
        assertThat(a.getEffectiveTo()).isEqualTo(NOW.plusSeconds(86400));
    }

    @Test
    void end_requiresReason() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW, "ref", NOW);
        assertThatThrownBy(() -> a.end(" ", NOW.plusSeconds(60), NOW))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void end_requiresEffectiveTo() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW, "ref", NOW);
        assertThatThrownBy(() -> a.end("r", null, NOW))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void end_effectiveToBeforeFrom_rejected() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW, "ref", NOW);
        assertThatThrownBy(() -> a.end("r", NOW.minusSeconds(60), NOW))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void end_fromProposed_rejected() {
        DpoAppointment a = proposeInternal();
        assertThatThrownBy(() -> a.end("r", NOW, NOW))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void cancel_fromProposed_succeeds() {
        DpoAppointment a = proposeInternal();
        a.cancel("Recrutement annulé", NOW.plusSeconds(60));
        assertThat(a.getStatus()).isEqualTo(DpoAppointmentStatus.CANCELLED);
        assertThat(a.isTerminal()).isTrue();
    }

    @Test
    void cancel_fromActive_rejected() {
        DpoAppointment a = proposeInternal();
        a.activate(NOW, NOW, "ref", NOW);
        assertThatThrownBy(() -> a.cancel("r", NOW))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    @Test
    void cancel_blankReason_rejected() {
        DpoAppointment a = proposeInternal();
        assertThatThrownBy(() -> a.cancel("  ", NOW))
                .isInstanceOf(DpoAppointmentStateException.class);
    }

    private DpoAppointment proposeInternal() {
        return DpoAppointment.propose(T, "DPO-2026-001",
                "Jane Doe", "dpo@example.com", "+33 1 23 45 67 89",
                DpoType.INTERNAL, null, "CIPP/E, ISO 27701 Lead Auditor",
                "GROUP", Set.of(), U, NOW);
    }
}
