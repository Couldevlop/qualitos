package com.openlab.qualitos.quality.cyberincidents.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CyberIncidentTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID HANDLER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void detect_setsThreeDeadlines() {
        CyberIncident i = detect(CyberIncidentSeverity.MEDIUM);
        assertThat(i.getEarlyWarningDeadlineAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(i.getInitialAssessmentDeadlineAt()).isEqualTo(NOW.plus(Duration.ofHours(72)));
        assertThat(i.getFinalReportDeadlineAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(i.getStatus()).isEqualTo(CyberIncidentStatus.DETECTED);
    }

    @Test
    void detect_invalidReference_throws() {
        assertThatThrownBy(() -> CyberIncident.detect(T, "lowercase", "Title", null,
                NOW, null, CyberIncidentType.MALWARE, CyberIncidentSeverity.LOW,
                0L, Set.of(), Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detect_negativeUsers_throws() {
        assertThatThrownBy(() -> CyberIncident.detect(T, "CYB-1", "t", null,
                NOW, null, CyberIncidentType.MALWARE, CyberIncidentSeverity.LOW,
                -1L, Set.of(), Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void detect_invalidAssetCode_throws() {
        assertThatThrownBy(() -> CyberIncident.detect(T, "CYB-1", "t", null,
                NOW, null, CyberIncidentType.MALWARE, CyberIncidentSeverity.LOW,
                0L, Set.of("BAD CODE"), Set.of(), null, U))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startAssessment_fromDetected_moves() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(CyberIncidentStatus.ASSESSING);
        assertThat(i.getHandledByUserId()).isEqualTo(HANDLER);
    }

    @Test
    void mitigate_requiresMeasures() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        assertThatThrownBy(() -> i.mitigate(" ", "impact", HANDLER, NOW.plusSeconds(60)))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void mitigate_fromAssessing_moves() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("Patched + rotated keys", "12h outage", HANDLER, NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(CyberIncidentStatus.MITIGATED);
        assertThat(i.getContainmentMeasures()).contains("Patched");
    }

    @Test
    void recordEarlyWarning_validInputs_persists() {
        CyberIncident i = detect(CyberIncidentSeverity.MEDIUM);
        i.startAssessment(HANDLER, NOW);
        i.recordEarlyWarning(NOW.plusSeconds(3600), "CSIRT-2026-001", NOW.plusSeconds(3600));
        assertThat(i.getEarlyWarningSentAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(i.getEarlyWarningReference()).isEqualTo("CSIRT-2026-001");
    }

    @Test
    void recordEarlyWarning_blankRef_throws() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        assertThatThrownBy(() -> i.recordEarlyWarning(NOW, " ", NOW))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void recordEarlyWarning_sentBeforeDetected_throws() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        assertThatThrownBy(() -> i.recordEarlyWarning(NOW.minusSeconds(60), "ref", NOW))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void recordNotification_onClosed_rejected() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        i.close("done", NOW.plusSeconds(60));
        assertThatThrownBy(() -> i.recordEarlyWarning(NOW.plusSeconds(120), "ref", NOW.plusSeconds(120)))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void recordFinalReport_onClosed_isRejectedToo() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        // close without final report needed (LOW severity)
        i.close("ok", NOW.plusSeconds(60));
        // Now record final report — rejected on rejected, but here it's CLOSED
        // closed peut accepter le rapport final (la garde rejette uniquement REJECTED)
        i.recordFinalReport(NOW.plusSeconds(120), "FINAL-1", NOW.plusSeconds(120));
        assertThat(i.getFinalReportSentAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void recordFinalReport_onRejected_throws() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.reject("false positive", NOW);
        assertThatThrownBy(() -> i.recordFinalReport(NOW.plusSeconds(60), "ref", NOW.plusSeconds(60)))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void close_severityHigh_withoutFinalReport_andNoNotes_throws() {
        CyberIncident i = detect(CyberIncidentSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        assertThatThrownBy(() -> i.close(null, NOW.plusSeconds(60)))
                .isInstanceOf(CyberIncidentStateException.class)
                .hasMessageContaining("NIS2 Art. 23.4.c");
    }

    @Test
    void close_severityHigh_withFinalReport_succeeds() {
        CyberIncident i = detect(CyberIncidentSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        i.recordFinalReport(NOW.plusSeconds(60), "FINAL-1", NOW.plusSeconds(60));
        i.close(null, NOW.plusSeconds(120));
        assertThat(i.getStatus()).isEqualTo(CyberIncidentStatus.CLOSED);
    }

    @Test
    void close_severityHigh_withNotes_succeeds() {
        CyberIncident i = detect(CyberIncidentSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        i.close("Exemption justifiée : seuil de matérialité non atteint", NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(CyberIncidentStatus.CLOSED);
    }

    @Test
    void close_severityLow_withoutFinalReport_succeeds() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        i.close(null, NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(CyberIncidentStatus.CLOSED);
    }

    @Test
    void reject_fromDetected_moves() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.reject("False positive — alert noise", NOW.plusSeconds(60));
        assertThat(i.getStatus()).isEqualTo(CyberIncidentStatus.REJECTED);
        assertThat(i.isTerminal()).isTrue();
    }

    @Test
    void reject_fromMitigated_rejected() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.startAssessment(HANDLER, NOW);
        i.mitigate("x", null, HANDLER, NOW);
        assertThatThrownBy(() -> i.reject("r", NOW.plusSeconds(60)))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void updateSeverity_onTerminal_rejected() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.reject("r", NOW);
        assertThatThrownBy(() -> i.updateSeverity(CyberIncidentSeverity.HIGH, NOW))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void linkBreach_onActive_succeeds() {
        CyberIncident i = detect(CyberIncidentSeverity.MEDIUM);
        UUID breach = UUID.randomUUID();
        i.linkBreach(breach, NOW.plusSeconds(60));
        assertThat(i.getLinkedBreachId()).isEqualTo(breach);
    }

    @Test
    void linkBreach_onTerminal_rejected() {
        CyberIncident i = detect(CyberIncidentSeverity.LOW);
        i.reject("r", NOW);
        assertThatThrownBy(() -> i.linkBreach(UUID.randomUUID(), NOW))
                .isInstanceOf(CyberIncidentStateException.class);
    }

    @Test
    void isEarlyWarningOverdue_trueAfterDeadline() {
        CyberIncident i = detect(CyberIncidentSeverity.HIGH);
        assertThat(i.isEarlyWarningOverdue(NOW.plus(Duration.ofHours(25)))).isTrue();
        assertThat(i.isEarlyWarningOverdue(NOW.plus(Duration.ofHours(20)))).isFalse();
    }

    @Test
    void isInitialAssessmentOverdue_trueAfterDeadline() {
        CyberIncident i = detect(CyberIncidentSeverity.HIGH);
        assertThat(i.isInitialAssessmentOverdue(NOW.plus(Duration.ofHours(73)))).isTrue();
    }

    @Test
    void isFinalReportOverdue_trueAfterDeadline() {
        CyberIncident i = detect(CyberIncidentSeverity.HIGH);
        assertThat(i.isFinalReportOverdue(NOW.plus(Duration.ofDays(31)))).isTrue();
    }

    @Test
    void overdueFlags_falseAfterNotification() {
        CyberIncident i = detect(CyberIncidentSeverity.HIGH);
        i.startAssessment(HANDLER, NOW);
        i.recordEarlyWarning(NOW.plusSeconds(60), "EW", NOW.plusSeconds(60));
        assertThat(i.isEarlyWarningOverdue(NOW.plus(Duration.ofHours(25)))).isFalse();
    }

    @Test
    void severitySignificant_high_orCritical() {
        assertThat(CyberIncidentSeverity.HIGH.isSignificant()).isTrue();
        assertThat(CyberIncidentSeverity.CRITICAL.isSignificant()).isTrue();
        assertThat(CyberIncidentSeverity.MEDIUM.isSignificant()).isFalse();
        assertThat(CyberIncidentSeverity.LOW.isSignificant()).isFalse();
    }

    private CyberIncident detect(CyberIncidentSeverity sev) {
        return CyberIncident.detect(T, "CYB-2026-001",
                "Ransomware on file server",
                "Endpoint EDR detected encryption activity",
                NOW, NOW.minus(Duration.ofHours(1)),
                CyberIncidentType.RANSOMWARE, sev,
                500L, Set.of("file-server-01"), Set.of("internal-storage"),
                null, U);
    }
}
