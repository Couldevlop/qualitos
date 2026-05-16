package com.openlab.qualitos.quality.breach.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BreachIncidentTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID H = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void detect_setsDpaDeadlineAtPlus72h() {
        BreachIncident i = detect(BreachSeverity.MEDIUM);
        assertThat(i.getStatus()).isEqualTo(BreachStatus.DETECTED);
        assertThat(i.getDpaDeadlineAt()).isEqualTo(NOW.plus(Duration.ofHours(72)));
        assertThat(i.isTerminal()).isFalse();
    }

    @Test
    void detect_invalidReference_throws() {
        assertThatThrownBy(() -> BreachIncident.detect(T, "lowercase-bad", "t", null,
                NOW, null, BreachSeverity.LOW, 0L, Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detect_invalidCategoryCode_throws() {
        assertThatThrownBy(() -> BreachIncident.detect(T, "BREACH-1", "t", null,
                NOW, null, BreachSeverity.LOW, 0L, Set.of("BAD CAT"), null, U))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detect_negativeCount_throws() {
        assertThatThrownBy(() -> BreachIncident.detect(T, "BREACH-1", "t", null,
                NOW, null, BreachSeverity.LOW, -1L, Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startAssessment_fromDetected_moves() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(BreachStatus.ASSESSING);
        assertThat(i.getHandledByUserId()).isEqualTo(H);
    }

    @Test
    void contain_requiresMeasures() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        assertThatThrownBy(() -> i.contain(" ", H, NOW))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void contain_fromAssessing_moves() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        i.contain("Reset passwords; rotate keys; revoke sessions", H, NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(BreachStatus.CONTAINED);
        assertThat(i.getContainmentMeasures()).contains("Reset");
    }

    @Test
    void notifyDpa_inContained_recordsTimestamp() {
        BreachIncident i = detect(BreachSeverity.MEDIUM);
        i.startAssessment(H, NOW);
        i.contain("steps", H, NOW.plusSeconds(60));
        i.notifyDpa(NOW.plusSeconds(120), "CNIL-2026-0001", NOW.plusSeconds(120));
        assertThat(i.getDpaNotifiedAt()).isEqualTo(NOW.plusSeconds(120));
        assertThat(i.getDpaReference()).isEqualTo("CNIL-2026-0001");
    }

    @Test
    void notifyDpa_beforeDetected_throws() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() -> i.notifyDpa(NOW.minusSeconds(60), "ref", NOW))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void notifyDpa_inDetected_throws() {
        BreachIncident i = detect(BreachSeverity.LOW);
        assertThatThrownBy(() -> i.notifyDpa(NOW, "ref", NOW))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void notifySubjects_inContained_recordsTimestampAndChannel() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        i.notifySubjects(NOW.plusSeconds(60), "email + in-app banner", NOW.plusSeconds(60));
        assertThat(i.getSubjectsNotifiedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(i.getSubjectsNotificationChannel()).contains("email");
    }

    @Test
    void close_severityHigh_withoutSubjectsNotif_requiresClosureNotes() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() -> i.close(null, NOW.plusSeconds(60)))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("subjects notification");
    }

    @Test
    void close_severityHigh_withClosureNotes_passes() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        i.close("Art. 34§3 exception : effective encryption rendered data unintelligible",
                NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(BreachStatus.CLOSED);
    }

    @Test
    void close_severityLow_withoutSubjectsNotif_passes() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        i.close(null, NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(BreachStatus.CLOSED);
    }

    @Test
    void reject_fromDetected_moves() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.reject("false positive — automated test trigger", NOW);
        assertThat(i.getStatus()).isEqualTo(BreachStatus.REJECTED);
        assertThat(i.isTerminal()).isTrue();
    }

    @Test
    void reject_fromContained_rejected() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() -> i.reject("r", NOW))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void updateSeverity_onTerminal_rejected() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.reject("r", NOW);
        assertThatThrownBy(() -> i.updateSeverity(BreachSeverity.HIGH, NOW))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void isDpaNotificationOverdue_trueAfterDeadline() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        assertThat(i.isDpaNotificationOverdue(NOW.plus(Duration.ofHours(73)))).isTrue();
        assertThat(i.isDpaNotificationOverdue(NOW.plus(Duration.ofHours(50)))).isFalse();
    }

    @Test
    void isDpaNotificationOverdue_falseAfterNotification() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        i.notifyDpa(NOW.plusSeconds(60), "ref", NOW);
        assertThat(i.isDpaNotificationOverdue(NOW.plus(Duration.ofHours(73)))).isFalse();
    }

    @Test
    void isSubjectNotificationRequired_highOrCritical() {
        assertThat(detect(BreachSeverity.LOW).isSubjectNotificationRequired()).isFalse();
        assertThat(detect(BreachSeverity.MEDIUM).isSubjectNotificationRequired()).isFalse();
        assertThat(detect(BreachSeverity.HIGH).isSubjectNotificationRequired()).isTrue();
        assertThat(detect(BreachSeverity.CRITICAL).isSubjectNotificationRequired()).isTrue();
    }

    private BreachIncident detect(BreachSeverity sev) {
        return BreachIncident.detect(T, "BREACH-2026-001", "Lost laptop",
                "Laptop with encrypted data lost in transit",
                NOW, NOW.minus(Duration.ofHours(2)),
                sev, 1500, Set.of("customer-pii", "billing"),
                "Risk of identification of customers", U);
    }
}
