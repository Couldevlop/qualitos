package com.openlab.qualitos.quality.aipmm.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PmmPlanTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID REVIEWER = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant LATER = NOW.plusSeconds(86400);

    @Test
    void draft_initialState() {
        PmmPlan p = draftReady();
        assertThat(p.isDraft()).isTrue();
        assertThat(p.getStatus()).isEqualTo(PmmPlanStatus.DRAFT);
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> PmmPlan.draft(T, "lowercase", SYS, "n", null,
                "metrics", "method", PmmReviewFrequency.MONTHLY, null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankName_throws() {
        assertThatThrownBy(() -> PmmPlan.draft(T, "REF-1", SYS, " ", null,
                "metrics", "method", PmmReviewFrequency.MONTHLY, null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_changesFields() {
        PmmPlan p = draftReady();
        p.editDraft("new", "desc2", "m2", "c2", PmmReviewFrequency.WEEKLY,
                "resp", "trigger", "qms-1", LATER);
        assertThat(p.getName()).isEqualTo("new");
        assertThat(p.getReviewFrequency()).isEqualTo(PmmReviewFrequency.WEEKLY);
    }

    @Test
    void edit_afterActivation_throws() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        assertThatThrownBy(() -> p.editDraft("x", null, "m", "c",
                PmmReviewFrequency.MONTHLY, null, null, null, LATER))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void activate_ok() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        assertThat(p.isActive()).isTrue();
        assertThat(p.getActivatedAt()).isEqualTo(NOW);
    }

    @Test
    void activate_missingMetrics_throws() {
        PmmPlan p = PmmPlan.draft(T, "REF-1", SYS, "n", null,
                null, "method", PmmReviewFrequency.MONTHLY, null, null, null, U, NOW);
        assertThatThrownBy(() -> p.activate(NOW))
                .isInstanceOf(PmmPlanStateException.class)
                .hasMessageContaining("metricsMonitored");
    }

    @Test
    void activate_missingCollection_throws() {
        PmmPlan p = PmmPlan.draft(T, "REF-1", SYS, "n", null,
                "metrics", null, PmmReviewFrequency.MONTHLY, null, null, null, U, NOW);
        assertThatThrownBy(() -> p.activate(NOW))
                .isInstanceOf(PmmPlanStateException.class)
                .hasMessageContaining("collectionMethod");
    }

    @Test
    void activate_missingFrequency_throws() {
        PmmPlan p = PmmPlan.draft(T, "REF-1", SYS, "n", null,
                "metrics", "method", null, null, null, null, U, NOW);
        assertThatThrownBy(() -> p.activate(NOW))
                .isInstanceOf(PmmPlanStateException.class)
                .hasMessageContaining("reviewFrequency");
    }

    @Test
    void activate_fromSuspended_clearsSuspensionMetadata() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        p.suspend("reason", NOW);
        p.activate(LATER);
        assertThat(p.isActive()).isTrue();
        assertThat(p.getSuspensionReason()).isNull();
        assertThat(p.getSuspendedAt()).isNull();
    }

    @Test
    void recordReview_ok() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        p.recordReview(REVIEWER, LATER);
        assertThat(p.getLastReviewedAt()).isEqualTo(LATER);
        assertThat(p.getLastReviewedByUserId()).isEqualTo(REVIEWER);
    }

    @Test
    void recordReview_whenDraft_throws() {
        PmmPlan p = draftReady();
        assertThatThrownBy(() -> p.recordReview(REVIEWER, NOW))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void recordReview_nullReviewer_throws() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        assertThatThrownBy(() -> p.recordReview(null, LATER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void suspend_fromActive_ok() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        p.suspend("maintenance", LATER);
        assertThat(p.isSuspended()).isTrue();
        assertThat(p.getSuspensionReason()).isEqualTo("maintenance");
    }

    @Test
    void suspend_fromDraft_throws() {
        PmmPlan p = draftReady();
        assertThatThrownBy(() -> p.suspend("x", NOW))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void suspend_blankReason_throws() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        assertThatThrownBy(() -> p.suspend(" ", LATER))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void close_fromActive_ok() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        p.close("end of life", LATER);
        assertThat(p.isClosed()).isTrue();
        assertThat(p.getEffectiveTo()).isEqualTo(LATER);
        assertThat(p.getClosureReason()).isEqualTo("end of life");
    }

    @Test
    void close_fromDraft_ok() {
        PmmPlan p = draftReady();
        p.close("never deployed", NOW);
        assertThat(p.isClosed()).isTrue();
    }

    @Test
    void close_blankReason_throws() {
        PmmPlan p = draftReady();
        assertThatThrownBy(() -> p.close(" ", NOW))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void close_terminal() {
        PmmPlan p = draftReady();
        p.close("r", NOW);
        assertThatThrownBy(() -> p.activate(LATER))
                .isInstanceOf(PmmPlanStateException.class);
    }

    @Test
    void nextReviewDueAt_basedOnActivated_whenNoReview() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        assertThat(p.nextReviewDueAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void nextReviewDueAt_basedOnLastReview_whenReviewed() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        p.recordReview(REVIEWER, LATER);
        assertThat(p.nextReviewDueAt()).isEqualTo(LATER.plus(Duration.ofDays(30)));
    }

    @Test
    void nextReviewDueAt_nullWhenNotActive() {
        PmmPlan p = draftReady();
        assertThat(p.nextReviewDueAt()).isNull();
    }

    @Test
    void isReviewOverdue_detectsAfterDue() {
        PmmPlan p = draftReady();
        p.activate(NOW);
        assertThat(p.isReviewOverdue(NOW.plus(Duration.ofDays(31)))).isTrue();
        assertThat(p.isReviewOverdue(NOW.plus(Duration.ofDays(10)))).isFalse();
    }

    @Test
    void frequencyPeriods_match() {
        assertThat(PmmReviewFrequency.WEEKLY.period()).isEqualTo(Duration.ofDays(7));
        assertThat(PmmReviewFrequency.MONTHLY.period()).isEqualTo(Duration.ofDays(30));
        assertThat(PmmReviewFrequency.QUARTERLY.period()).isEqualTo(Duration.ofDays(90));
        assertThat(PmmReviewFrequency.SEMI_ANNUAL.period()).isEqualTo(Duration.ofDays(182));
        assertThat(PmmReviewFrequency.ANNUAL.period()).isEqualTo(Duration.ofDays(365));
    }

    @Test
    void assignId() {
        PmmPlan p = draftReady();
        UUID id = UUID.randomUUID();
        p.assignId(id);
        assertThat(p.getId()).isEqualTo(id);
    }

    private static PmmPlan draftReady() {
        return PmmPlan.draft(T, "REF-1", SYS, "Name", "desc",
                "metrics", "method", PmmReviewFrequency.MONTHLY,
                "resp party", "trigger", "qms-1", U, NOW);
    }
}
