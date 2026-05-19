package com.openlab.qualitos.quality.breach.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
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

    // ---- Validation branches in notify* and reject ------------------------

    @Test
    void notifyDpa_nullNotifiedAt_throws() {
        BreachIncident i = detect(BreachSeverity.MEDIUM);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() -> i.notifyDpa(null, "ref", NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("notifiedAt");
    }

    @Test
    void notifyDpa_blankReference_throws() {
        BreachIncident i = detect(BreachSeverity.MEDIUM);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() -> i.notifyDpa(NOW.plusSeconds(60), "   ", NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("dpaReference");
    }

    @Test
    void notifyDpa_nullReference_throws() {
        BreachIncident i = detect(BreachSeverity.MEDIUM);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() -> i.notifyDpa(NOW.plusSeconds(60), null, NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("dpaReference");
    }

    @Test
    void notifyDpa_inAssessing_recordsTimestamp() {
        // Covers the ASSESSING branch of the status guard (vs CONTAINED already covered)
        BreachIncident i = detect(BreachSeverity.MEDIUM);
        i.startAssessment(H, NOW);
        i.notifyDpa(NOW.plusSeconds(30), "CNIL-001", NOW.plusSeconds(30));
        assertThat(i.getDpaNotifiedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void notifySubjects_inDetected_throws() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        assertThatThrownBy(() -> i.notifySubjects(NOW, "email", NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("ASSESSING or CONTAINED");
    }

    @Test
    void notifySubjects_nullNotifiedAt_throws() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        assertThatThrownBy(() -> i.notifySubjects(null, "email", NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("notifiedAt");
    }

    @Test
    void notifySubjects_blankChannel_throws() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        assertThatThrownBy(() -> i.notifySubjects(NOW.plusSeconds(60), "  ", NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("channel");
    }

    @Test
    void notifySubjects_nullChannel_throws() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        assertThatThrownBy(() -> i.notifySubjects(NOW.plusSeconds(60), null, NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("channel");
    }

    @Test
    void notifySubjects_beforeDetected_throws() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() ->
                i.notifySubjects(NOW.minusSeconds(60), "email", NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("cannot precede");
    }

    @Test
    void reject_blankReason_throws() {
        BreachIncident i = detect(BreachSeverity.LOW);
        assertThatThrownBy(() -> i.reject("  ", NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void reject_nullReason_throws() {
        BreachIncident i = detect(BreachSeverity.LOW);
        assertThatThrownBy(() -> i.reject(null, NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void reject_fromAssessing_moves() {
        // Covers the ASSESSING → REJECTED transition (vs only DETECTED→REJECTED before)
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        i.reject("benign anomaly after triage", NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(BreachStatus.REJECTED);
        assertThat(i.getRejectionReason()).contains("benign");
    }

    @Test
    void contain_withNullHandler_keepsExistingHandler() {
        // Covers the `handledByUserId != null` false branch in contain()
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        i.contain("isolated workstation", null, NOW.plusSeconds(60));
        assertThat(i.getHandledByUserId()).isEqualTo(H);
        assertThat(i.getStatus()).isEqualTo(BreachStatus.CONTAINED);
    }

    @Test
    void close_severityHigh_blankClosureNotes_alsoThrows() {
        // Covers the closureNotes.isBlank() side of the boolean OR
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        assertThatThrownBy(() -> i.close("   ", NOW.plusSeconds(60)))
                .isInstanceOf(BreachStateException.class);
    }

    @Test
    void isDpaNotificationOverdue_falseWhenRejected() {
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.reject("false positive", NOW);
        assertThat(i.isDpaNotificationOverdue(NOW.plus(Duration.ofHours(73)))).isFalse();
    }

    @Test
    void sanitizeCategories_skipsNullsAndBlanksAndTrims() {
        // Covers: null input → empty path is covered in detect calls; here we
        // cover the loop branches: null entry (continue), blank trimmed
        // (continue), and the trim() with surrounding whitespace.
        Set<String> input = new HashSet<>();
        input.add(null);
        input.add("");
        input.add("   ");
        input.add("  customer-pii  ");
        input.add("billing");
        BreachIncident i = BreachIncident.detect(T, "BREACH-NULLS",
                "title", null, NOW, null,
                BreachSeverity.LOW, 0L, input, null, U);
        assertThat(i.getAffectedDataCategories())
                .containsExactlyInAnyOrder("customer-pii", "billing");
    }

    @Test
    void sanitizeCategories_nullInput_emptySet() {
        BreachIncident i = BreachIncident.detect(T, "BREACH-NULL-SET",
                "title", null, NOW, null,
                BreachSeverity.LOW, 0L, null, null, U);
        assertThat(i.getAffectedDataCategories()).isEmpty();
    }

    @Test
    void title_overMaxLength_throws() {
        String tooLong = "x".repeat(251);
        assertThatThrownBy(() -> BreachIncident.detect(T, "BREACH-LONG",
                tooLong, null, NOW, null,
                BreachSeverity.LOW, 0L, Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void title_blank_throws() {
        assertThatThrownBy(() -> BreachIncident.detect(T, "BREACH-BLANK",
                "  ", null, NOW, null,
                BreachSeverity.LOW, 0L, Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    @Test
    void constructor_nullUpdatedAt_fallsBackToDetectedAt() {
        BreachIncident i = new BreachIncident(
                null, T, "BREACH-FALL",
                "title", null,
                NOW, null, NOW.plus(Duration.ofHours(72)),
                BreachSeverity.LOW, BreachStatus.DETECTED,
                0L, Set.of(),
                null, null, null, null, null, null, null, null,
                U, null, null,
                /* updatedAt = */ null);
        assertThat(i.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void assignId_setsId() {
        BreachIncident i = detect(BreachSeverity.LOW);
        UUID newId = UUID.randomUUID();
        i.assignId(newId);
        assertThat(i.getId()).isEqualTo(newId);
    }

    @Test
    void contain_nullMeasures_throws() {
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        assertThatThrownBy(() -> i.contain(null, H, NOW))
                .isInstanceOf(BreachStateException.class)
                .hasMessageContaining("containmentMeasures");
    }

    @Test
    void close_severityHigh_withSubjectsNotified_passesWithoutClosureNotes() {
        // Covers the subjectsNotifMissing=false branch in close()
        BreachIncident i = detect(BreachSeverity.HIGH);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        i.notifySubjects(NOW.plusSeconds(60), "email", NOW.plusSeconds(60));
        i.close(null, NOW.plusSeconds(120));
        assertThat(i.getStatus()).isEqualTo(BreachStatus.CLOSED);
    }

    @Test
    void isTerminal_trueWhenClosed() {
        // Covers the CLOSED branch of isTerminal (REJECTED already covered)
        BreachIncident i = detect(BreachSeverity.LOW);
        i.startAssessment(H, NOW);
        i.contain("x", H, NOW);
        i.close(null, NOW.plusSeconds(60));
        assertThat(i.isTerminal()).isTrue();
    }

    @Test
    void detect_nullReference_throws() {
        // Covers the v == null branch of requireReference
        assertThatThrownBy(() -> BreachIncident.detect(T, null, "title", null,
                NOW, null, BreachSeverity.LOW, 0L, Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("internalReference");
    }

    @Test
    void detect_nullTitle_throws() {
        // Covers the v == null branch of requireText
        assertThatThrownBy(() -> BreachIncident.detect(T, "BREACH-NULL-TITLE",
                null, null, NOW, null, BreachSeverity.LOW, 0L, Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
    }

    private BreachIncident detect(BreachSeverity sev) {
        return BreachIncident.detect(T, "BREACH-2026-001", "Lost laptop",
                "Laptop with encrypted data lost in transit",
                NOW, NOW.minus(Duration.ofHours(2)),
                sev, 1500, Set.of("customer-pii", "billing"),
                "Risk of identification of customers", U);
    }
}
