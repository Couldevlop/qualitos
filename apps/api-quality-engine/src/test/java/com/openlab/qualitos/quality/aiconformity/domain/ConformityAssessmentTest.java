package com.openlab.qualitos.quality.aiconformity.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConformityAssessmentTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final UUID QMS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    static final Instant LATER = NOW.plusSeconds(86400);
    static final Instant VALID_UNTIL = NOW.plusSeconds(365L * 86400);

    @Test
    void plan_internal_ok() {
        ConformityAssessment a = planInternal();
        assertThat(a.isPlanned()).isTrue();
        assertThat(a.getProcedure()).isEqualTo(ConformityProcedure.INTERNAL_CONTROL);
    }

    @Test
    void plan_notifiedBody_ok() {
        ConformityAssessment a = planNotified();
        assertThat(a.getNotifiedBodyId()).isEqualTo("1234");
    }

    @Test
    void plan_notifiedBody_missingId_throws() {
        assertThatThrownBy(() -> ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                ConformityProcedure.NOTIFIED_BODY, null, "Body", "scope", U, NOW))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void plan_notifiedBody_missingName_throws() {
        assertThatThrownBy(() -> ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                ConformityProcedure.NOTIFIED_BODY, "1234", " ", "scope", U, NOW))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void plan_invalidNotifiedBodyId_throws() {
        assertThatThrownBy(() -> ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                ConformityProcedure.NOTIFIED_BODY, "abc", "Body", "scope", U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plan_invalidReference_throws() {
        assertThatThrownBy(() -> ConformityAssessment.plan(T, "lowercase", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, "scope", U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plan_blankScope_throws() {
        assertThatThrownBy(() -> ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null, " ", U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_planned_ok() {
        ConformityAssessment a = planInternal();
        UUID newQms = UUID.randomUUID();
        a.editPlanned(newQms, null, null, "new scope", LATER);
        assertThat(a.getQmsId()).isEqualTo(newQms);
        assertThat(a.getScope()).isEqualTo("new scope");
    }

    @Test
    void edit_afterStart_throws() {
        ConformityAssessment a = planInternal();
        a.start(LATER);
        assertThatThrownBy(() -> a.editPlanned(QMS, null, null, "x", LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void edit_notifiedBody_missingId_throws() {
        ConformityAssessment a = planNotified();
        assertThatThrownBy(() -> a.editPlanned(QMS, null, "Body", "x", LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void start_ok() {
        ConformityAssessment a = planInternal();
        a.start(LATER);
        assertThat(a.isInProgress()).isTrue();
        assertThat(a.getStartedAt()).isEqualTo(LATER);
    }

    @Test
    void start_alreadyStarted_throws() {
        ConformityAssessment a = planInternal();
        a.start(LATER);
        assertThatThrownBy(() -> a.start(LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void certify_ok() {
        ConformityAssessment a = planNotified();
        a.start(LATER);
        a.certify("CERT-001", "EU-DECL-001", VALID_UNTIL, LATER);
        assertThat(a.isCertified()).isTrue();
        assertThat(a.getCertificateNumber()).isEqualTo("CERT-001");
        assertThat(a.getValidUntil()).isEqualTo(VALID_UNTIL);
    }

    @Test
    void certify_blankCert_throws() {
        ConformityAssessment a = planNotified();
        a.start(LATER);
        assertThatThrownBy(() -> a.certify(" ", "EU-1", VALID_UNTIL, LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void certify_blankDeclaration_throws() {
        ConformityAssessment a = planNotified();
        a.start(LATER);
        assertThatThrownBy(() -> a.certify("CERT-1", " ", VALID_UNTIL, LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void certify_nullValidUntil_throws() {
        ConformityAssessment a = planNotified();
        a.start(LATER);
        assertThatThrownBy(() -> a.certify("CERT-1", "EU-1", null, LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void certify_validUntilInPast_throws() {
        ConformityAssessment a = planNotified();
        a.start(LATER);
        assertThatThrownBy(() -> a.certify("CERT-1", "EU-1", LATER.minusSeconds(1), LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void certify_fromPlanned_throws() {
        ConformityAssessment a = planNotified();
        assertThatThrownBy(() -> a.certify("CERT-1", "EU-1", VALID_UNTIL, LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void markExpired_ok() {
        ConformityAssessment a = certified();
        a.markExpired(VALID_UNTIL.plusSeconds(1));
        assertThat(a.isTerminal()).isTrue();
        assertThat(a.getStatus()).isEqualTo(ConformityAssessmentStatus.EXPIRED);
    }

    @Test
    void markExpired_beforeValidUntil_throws() {
        ConformityAssessment a = certified();
        assertThatThrownBy(() -> a.markExpired(LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void markExpired_fromPlanned_throws() {
        ConformityAssessment a = planInternal();
        assertThatThrownBy(() -> a.markExpired(LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void revoke_fromCertified_ok() {
        ConformityAssessment a = certified();
        a.revoke("misuse", LATER);
        assertThat(a.getStatus()).isEqualTo(ConformityAssessmentStatus.REVOKED);
    }

    @Test
    void revoke_fromPlanned_ok() {
        ConformityAssessment a = planInternal();
        a.revoke("cancelled", LATER);
        assertThat(a.isTerminal()).isTrue();
    }

    @Test
    void revoke_blankReason_throws() {
        ConformityAssessment a = planInternal();
        assertThatThrownBy(() -> a.revoke(" ", LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void markFailed_fromInProgress_ok() {
        ConformityAssessment a = planNotified();
        a.start(LATER);
        a.markFailed("non-conformities", LATER);
        assertThat(a.isTerminal()).isTrue();
        assertThat(a.getFailureReason()).isEqualTo("non-conformities");
    }

    @Test
    void markFailed_fromCertified_throws() {
        ConformityAssessment a = certified();
        assertThatThrownBy(() -> a.markFailed("x", LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void markFailed_blankReason_throws() {
        ConformityAssessment a = planInternal();
        assertThatThrownBy(() -> a.markFailed(" ", LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void isCertificateExpired_detectsExpiry() {
        ConformityAssessment a = certified();
        assertThat(a.isCertificateExpired(VALID_UNTIL.plusSeconds(1))).isTrue();
        assertThat(a.isCertificateExpired(LATER)).isFalse();
    }

    @Test
    void isCertificateExpired_falseIfNotCertified() {
        ConformityAssessment a = planInternal();
        assertThat(a.isCertificateExpired(VALID_UNTIL.plusSeconds(1))).isFalse();
    }

    @Test
    void terminalState_cannotTransition() {
        ConformityAssessment a = planInternal();
        a.revoke("x", LATER);
        assertThatThrownBy(() -> a.start(LATER))
                .isInstanceOf(ConformityAssessmentStateException.class);
    }

    @Test
    void assignId() {
        ConformityAssessment a = planInternal();
        UUID id = UUID.randomUUID();
        a.assignId(id);
        assertThat(a.getId()).isEqualTo(id);
    }

    private static ConformityAssessment planInternal() {
        return ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                ConformityProcedure.INTERNAL_CONTROL, null, null,
                "scope of assessment", U, NOW);
    }

    private static ConformityAssessment planNotified() {
        return ConformityAssessment.plan(T, "REF-1", SYS, QMS,
                ConformityProcedure.NOTIFIED_BODY, "1234", "TÜV",
                "scope of assessment", U, NOW);
    }

    private static ConformityAssessment certified() {
        ConformityAssessment a = planNotified();
        a.start(LATER);
        a.certify("CERT-001", "EU-DECL-001", VALID_UNTIL, LATER);
        return a;
    }
}
