package com.openlab.qualitos.quality.nis2measures.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class Nis2RiskMeasureTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void plan_validInputs_createsPlanned() {
        Nis2RiskMeasure m = planLow();
        assertThat(m.isPlanned()).isTrue();
        assertThat(m.getMaturityLevel()).isEqualTo(2);
    }

    @Test
    void plan_invalidReference_throws() {
        assertThatThrownBy(() -> Nis2RiskMeasure.plan(T, "lowercase",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plan_invalidMaturity_throws() {
        assertThatThrownBy(() -> Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 0, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 6, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(), null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plan_invalidReviewInterval_throws() {
        assertThatThrownBy(() -> Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 2, ResidualRiskRating.LOW, null, 29 /* < 30 */,
                Set.of(), Set.of(), Set.of(), null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 2, ResidualRiskRating.LOW, null, 1096 /* > 1095 */,
                Set.of(), Set.of(), Set.of(), null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void plan_criticalRiskWithoutJustification_throws() {
        assertThatThrownBy(() -> Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 2, ResidualRiskRating.CRITICAL, null, 365,
                Set.of(), Set.of(), Set.of(), null, U, NOW))
                .isInstanceOf(Nis2MeasureStateException.class)
                .hasMessageContaining("CRITICAL");
    }

    @Test
    void plan_criticalRiskWithJustification_ok() {
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 1, ResidualRiskRating.CRITICAL,
                "Justification : composant legacy en cours de remplacement Q3 2026",
                365, Set.of(), Set.of(), Set.of(), null, U, NOW);
        assertThat(m.getResidualRiskRating()).isEqualTo(ResidualRiskRating.CRITICAL);
    }

    @Test
    void plan_invalidUrl_throws() {
        assertThatThrownBy(() -> Nis2RiskMeasure.plan(T, "M-1",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                U, 2, ResidualRiskRating.LOW, null, 365,
                Set.of("not-a-url"), Set.of(), Set.of(), null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_updatesFields() {
        Nis2RiskMeasure m = planLow();
        m.edit("Updated", "desc", U, 4, ResidualRiskRating.MEDIUM, null, 180,
                Set.of("https://x.com/evidence.pdf"), Set.of(), Set.of(),
                "updated notes", NOW.plusSeconds(60));
        assertThat(m.getTitle()).isEqualTo("Updated");
        assertThat(m.getMaturityLevel()).isEqualTo(4);
        assertThat(m.getReviewIntervalDays()).isEqualTo(180);
        assertThat(m.getEvidenceUrls()).contains("https://x.com/evidence.pdf");
    }

    @Test
    void edit_onDeprecated_throws() {
        Nis2RiskMeasure m = planLow();
        m.deprecate(NOW);
        assertThatThrownBy(() -> m.edit("X", null, U, 2, ResidualRiskRating.LOW,
                null, 365, Set.of(), Set.of(), Set.of(), null, NOW.plusSeconds(60)))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void startImplementation_setsEffectiveFromOnce() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW.plusSeconds(60));
        assertThat(m.getStatus()).isEqualTo(Nis2MeasureStatus.IN_PROGRESS);
        assertThat(m.getEffectiveFrom()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void markImplemented_fromInProgress_moves() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW);
        m.markImplemented(NOW.plusSeconds(60));
        assertThat(m.getStatus()).isEqualTo(Nis2MeasureStatus.IMPLEMENTED);
    }

    @Test
    void verify_setsReviewMetadataAndNextDueDate() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        m.verify(REVIEWER, NOW.plusSeconds(60), NOW.plusSeconds(60));
        assertThat(m.isVerified()).isTrue();
        assertThat(m.getLastReviewedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(m.getReviewedByUserId()).isEqualTo(REVIEWER);
        assertThat(m.getNextReviewDueAt()).isEqualTo(
                NOW.plusSeconds(60).plus(Duration.ofDays(365)));
    }

    @Test
    void verify_requiresReviewer() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        assertThatThrownBy(() -> m.verify(null, NOW, NOW))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void verify_requiresReviewedAt() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        assertThatThrownBy(() -> m.verify(REVIEWER, null, NOW))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void review_refreshesNextDueDate() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        m.verify(REVIEWER, NOW, NOW);
        Instant later = NOW.plus(Duration.ofDays(400));
        m.review(REVIEWER, later, later);
        assertThat(m.getLastReviewedAt()).isEqualTo(later);
        assertThat(m.getNextReviewDueAt()).isEqualTo(later.plus(Duration.ofDays(365)));
    }

    @Test
    void review_onNonVerified_rejected() {
        Nis2RiskMeasure m = planLow();
        assertThatThrownBy(() -> m.review(REVIEWER, NOW, NOW))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void deprecate_fromPlanned_succeeds() {
        Nis2RiskMeasure m = planLow();
        m.deprecate(NOW);
        assertThat(m.isTerminal()).isTrue();
        assertThat(m.getEffectiveTo()).isEqualTo(NOW);
    }

    @Test
    void transitionGuards_invalidPath_rejected() {
        Nis2RiskMeasure m = planLow();
        assertThatThrownBy(() -> m.markImplemented(NOW))
                .isInstanceOf(Nis2MeasureStateException.class);
        assertThatThrownBy(() -> m.verify(REVIEWER, NOW, NOW))
                .isInstanceOf(Nis2MeasureStateException.class);
    }

    @Test
    void isReviewOverdue_trueAfterDueDate() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        m.verify(REVIEWER, NOW, NOW);
        // nextReviewDueAt = NOW + 365 days
        assertThat(m.isReviewOverdue(NOW.plus(Duration.ofDays(366)))).isTrue();
        assertThat(m.isReviewOverdue(NOW.plus(Duration.ofDays(300)))).isFalse();
    }

    @Test
    void isReviewOverdue_falseOnDeprecated() {
        Nis2RiskMeasure m = planLow();
        m.startImplementation(NOW);
        m.markImplemented(NOW);
        m.verify(REVIEWER, NOW, NOW);
        m.deprecate(NOW.plus(Duration.ofDays(400)));
        assertThat(m.isReviewOverdue(NOW.plus(Duration.ofDays(500)))).isFalse();
    }

    @Test
    void nullSetsAndNulls_handled() {
        Nis2RiskMeasure m = Nis2RiskMeasure.plan(T, "M-N",
                Nis2MeasureCategory.CRYPTOGRAPHY, "t", null,
                null /* no owner */, 1, ResidualRiskRating.LOW, null, 365,
                null, null, null, null, U, NOW);
        assertThat(m.getEvidenceUrls()).isEmpty();
        assertThat(m.getLinkedProcessingActivityIds()).isEmpty();
        assertThat(m.getLinkedProcessorAgreementIds()).isEmpty();
    }

    private Nis2RiskMeasure planLow() {
        return Nis2RiskMeasure.plan(T, "M-2026-001",
                Nis2MeasureCategory.MFA_AND_COMMUNICATIONS,
                "MFA on all admin accounts", "TOTP + WebAuthn",
                U, 2, ResidualRiskRating.LOW, null, 365,
                Set.of(), Set.of(), Set.of(),
                "Initial rollout 50% complete", U, NOW);
    }
}
