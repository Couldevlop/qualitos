package com.openlab.qualitos.quality.aiincidents.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiIncidentTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant OCCURRED = Instant.parse("2026-05-16T08:00:00Z");
    static final Instant DETECTED = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant NOW = Instant.parse("2026-05-16T11:00:00Z");
    static final Instant LATER = NOW.plusSeconds(86400);

    @Test
    void detect_initialState() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        assertThat(i.isDetected()).isTrue();
        assertThat(i.getStatus()).isEqualTo(AiIncidentStatus.DETECTED);
    }

    @Test
    void detect_invalidReference_throws() {
        assertThatThrownBy(() -> AiIncident.detect(T, "lowercase", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", null, null, OCCURRED, DETECTED, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detect_occurredAfterDetected_throws() {
        assertThatThrownBy(() -> AiIncident.detect(T, "REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                "desc", null, null, DETECTED, OCCURRED, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detect_blankDescription_throws() {
        assertThatThrownBy(() -> AiIncident.detect(T, "REF-1", SYS,
                AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION,
                " ", null, null, OCCURRED, DETECTED, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_changesDescription() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.editDetected("updated", "persons", "actions", LATER);
        assertThat(i.getDescription()).isEqualTo("updated");
        assertThat(i.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void edit_afterInvestigation_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        assertThatThrownBy(() -> i.editDetected("x", null, null, LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void startInvestigation_ok() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        assertThat(i.getStatus()).isEqualTo(AiIncidentStatus.INVESTIGATING);
        assertThat(i.getInvestigationLeadUserId()).isEqualTo(LEAD);
    }

    @Test
    void startInvestigation_nullLead_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        assertThatThrownBy(() -> i.startInvestigation(null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void notifyRegulator_ok() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "root cause", "actions", LATER);
        assertThat(i.isNotifiedRegulator()).isTrue();
        assertThat(i.getRegulatorReference()).isEqualTo("REG-1");
    }

    @Test
    void notifyRegulator_missingRef_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        assertThatThrownBy(() -> i.notifyRegulator(" ", "rca", null, LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void notifyRegulator_missingRca_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        assertThatThrownBy(() -> i.notifyRegulator("REG-1", null, "x", LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void notifyRegulator_fromDetected_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        assertThatThrownBy(() -> i.notifyRegulator("R", "rca", null, LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void close_ok() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "rca", "actions", LATER);
        i.close("corrective actions taken", LATER);
        assertThat(i.isClosed()).isTrue();
        assertThat(i.isTerminal()).isTrue();
    }

    @Test
    void close_missingActions_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "rca", null, LATER);
        assertThatThrownBy(() -> i.close(" ", LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void close_fromDetected_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        assertThatThrownBy(() -> i.close("actions", LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void dismiss_fromDetected_ok() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.dismiss("false alarm", NOW);
        assertThat(i.isDismissed()).isTrue();
        assertThat(i.getDismissalReason()).isEqualTo("false alarm");
    }

    @Test
    void dismiss_fromInvestigating_ok() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        i.dismiss("not an AI failure", LATER);
        assertThat(i.isDismissed()).isTrue();
    }

    @Test
    void dismiss_fromNotified_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "rca", null, LATER);
        assertThatThrownBy(() -> i.dismiss("x", LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void dismiss_blankReason_throws() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        assertThatThrownBy(() -> i.dismiss(" ", NOW))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void terminal_cannotTransition() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        i.dismiss("r", NOW);
        assertThatThrownBy(() -> i.startInvestigation(LEAD, LATER))
                .isInstanceOf(AiIncidentStateException.class);
    }

    @Test
    void regulatorNotificationDueAt_addsDeadline() {
        AiIncident i = detect(AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH);
        assertThat(i.regulatorNotificationDueAt())
                .isEqualTo(DETECTED.plus(Duration.ofDays(2)));
    }

    @Test
    void isOverdue_detectedBeyondDeadline() {
        AiIncident i = detect(AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH);
        Instant after = DETECTED.plus(Duration.ofDays(3));
        assertThat(i.isRegulatorNotificationOverdue(after)).isTrue();
    }

    @Test
    void isOverdue_withinDeadline_false() {
        AiIncident i = detect(AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH);
        Instant within = DETECTED.plus(Duration.ofHours(12));
        assertThat(i.isRegulatorNotificationOverdue(within)).isFalse();
    }

    @Test
    void isOverdue_afterNotification_false() {
        AiIncident i = detect(AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH);
        i.startInvestigation(LEAD, NOW);
        i.notifyRegulator("REG-1", "rca", null, NOW);
        Instant far = DETECTED.plus(Duration.ofDays(100));
        assertThat(i.isRegulatorNotificationOverdue(far)).isFalse();
    }

    @Test
    void severityDeadlines_match() {
        assertThat(AiIncidentSeverity.DEATH_OR_SERIOUS_HARM_TO_HEALTH
                .regulatorNotificationDeadline()).isEqualTo(Duration.ofDays(2));
        assertThat(AiIncidentSeverity.SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS
                .regulatorNotificationDeadline()).isEqualTo(Duration.ofDays(10));
        assertThat(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION
                .regulatorNotificationDeadline()).isEqualTo(Duration.ofDays(15));
        assertThat(AiIncidentSeverity.SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE
                .regulatorNotificationDeadline()).isEqualTo(Duration.ofDays(15));
    }

    @Test
    void assignId() {
        AiIncident i = detect(AiIncidentSeverity.CRITICAL_INFRASTRUCTURE_DISRUPTION);
        UUID id = UUID.randomUUID();
        i.assignId(id);
        assertThat(i.getId()).isEqualTo(id);
    }

    private static AiIncident detect(AiIncidentSeverity severity) {
        return AiIncident.detect(T, "REF-1", SYS, severity,
                "description", "affected", "immediate",
                OCCURRED, DETECTED, U, NOW);
    }
}
