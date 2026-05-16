package com.openlab.qualitos.quality.ehs.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests purs sur l'agrégat domaine — pas de Spring, pas de JPA, instantanés.
 */
class IncidentTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-05-15T10:00:00Z");
    static final Instant T1 = Instant.parse("2026-05-15T11:00:00Z");

    @Test
    void report_defaultsSeverityMediumAndStatusReported() {
        Incident i = Incident.report(TENANT, "EHS-1", "Fall", null,
                IncidentType.INJURY, null, null, "Floor 2", USER, T0);
        assertThat(i.getStatus()).isEqualTo(IncidentStatus.REPORTED);
        assertThat(i.getSeverity()).isEqualTo(IncidentSeverity.MEDIUM);
        assertThat(i.getOccurredAt()).isEqualTo(T0);
        assertThat(i.getReportedAt()).isEqualTo(T0);
    }

    @Test
    void report_preservesOccurredAtIfProvided() {
        Instant occ = Instant.parse("2026-05-10T08:00:00Z");
        Incident i = Incident.report(TENANT, "EHS-1", "x", null,
                IncidentType.NEAR_MISS, IncidentSeverity.HIGH, occ, null, USER, T0);
        assertThat(i.getOccurredAt()).isEqualTo(occ);
        assertThat(i.getSeverity()).isEqualTo(IncidentSeverity.HIGH);
    }

    @Test
    void investigate_fromReported_ok() {
        Incident i = sample();
        i.investigate(UUID.randomUUID(), T1);
        assertThat(i.getStatus()).isEqualTo(IncidentStatus.INVESTIGATING);
        assertThat(i.getOwnerUserId()).isNotNull();
    }

    @Test
    void investigate_fromMitigated_rejected() {
        Incident i = sample();
        i.investigate(null, T1);
        i.mitigate("rc", "ca", T1);
        assertThatThrownBy(() -> i.investigate(null, T1))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void mitigate_requiresRootCauseAndActions() {
        Incident i = sample();
        i.investigate(null, T1);
        assertThatThrownBy(() -> i.mitigate(null, "ca", T1))
                .isInstanceOf(IncidentStateException.class);
        assertThatThrownBy(() -> i.mitigate("rc", null, T1))
                .isInstanceOf(IncidentStateException.class);
        assertThatThrownBy(() -> i.mitigate("", "ca", T1))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void mitigate_fromReported_rejected() {
        Incident i = sample();
        assertThatThrownBy(() -> i.mitigate("rc", "ca", T1))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void close_fromMitigated_ok() {
        Incident i = sample();
        i.investigate(null, T1);
        i.mitigate("rc", "ca", T1);
        i.close(T1);
        assertThat(i.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(i.getClosedAt()).isEqualTo(T1);
    }

    @Test
    void close_fromInvestigating_rejected() {
        Incident i = sample();
        i.investigate(null, T1);
        assertThatThrownBy(() -> i.close(T1)).isInstanceOf(IncidentStateException.class);
    }

    @Test
    void cancel_fromReported_ok() {
        Incident i = sample();
        i.cancel(T1);
        assertThat(i.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
    }

    @Test
    void cancel_fromInvestigating_ok() {
        Incident i = sample();
        i.investigate(null, T1);
        i.cancel(T1);
        assertThat(i.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
    }

    @Test
    void cancel_fromMitigated_rejected() {
        Incident i = sample();
        i.investigate(null, T1);
        i.mitigate("rc", "ca", T1);
        assertThatThrownBy(() -> i.cancel(T1)).isInstanceOf(IncidentStateException.class);
    }

    @Test
    void linkCapa_onTerminal_rejected() {
        Incident i = sample();
        i.cancel(T1);
        assertThatThrownBy(() -> i.linkCapa(UUID.randomUUID(), T1))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void linkNc_onActive_ok() {
        Incident i = sample();
        UUID nc = UUID.randomUUID();
        i.linkNc(nc, T1);
        assertThat(i.getNcId()).isEqualTo(nc);
    }

    @Test
    void editDetails_terminal_rejected() {
        Incident i = sample();
        i.cancel(T1);
        assertThatThrownBy(() -> i.editDetails("x", null, null, null, null, null, T1))
                .isInstanceOf(IncidentStateException.class);
    }

    @Test
    void editDetails_appliesPatches() {
        Incident i = sample();
        i.editDetails("New title", "desc", "Loc B", "PB", IncidentSeverity.HIGH, "iso-14001", T1);
        assertThat(i.getTitle()).isEqualTo("New title");
        assertThat(i.getSeverity()).isEqualTo(IncidentSeverity.HIGH);
        assertThat(i.getStandardsCsv()).isEqualTo("iso-14001");
    }

    @Test
    void isTerminal_trueForClosedAndCancelled() {
        Incident a = sample(); a.cancel(T1);
        Incident b = sample(); b.investigate(null, T1); b.mitigate("rc", "ca", T1); b.close(T1);
        assertThat(a.isTerminal()).isTrue();
        assertThat(b.isTerminal()).isTrue();
    }

    @Test
    void constructor_rejectsNullRequiredFields() {
        assertThatThrownBy(() -> new Incident(null, null, "c", "t", null,
                IncidentType.INJURY, null, null, T0, T0, null, null,
                null, null, null, null, null, null, null, null, USER, T0, T0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void assignId_setsIdAfterPersist() {
        Incident i = sample();
        UUID id = UUID.randomUUID();
        i.assignId(id);
        assertThat(i.getId()).isEqualTo(id);
    }

    private Incident sample() {
        return Incident.report(TENANT, "EHS-1", "Title", "desc",
                IncidentType.INJURY, IncidentSeverity.MEDIUM,
                T0, "Site A", USER, T0);
    }
}
