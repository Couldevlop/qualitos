package com.openlab.qualitos.quality.retention.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class RetentionRuleTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void draft_validInputs_createsDraft() {
        RetentionRule r = draft();
        assertThat(r.isDraft()).isTrue();
        assertThat(r.getEffectiveFrom()).isNull();
        assertThat(r.getStatus()).isEqualTo(RetentionRuleStatus.DRAFT);
    }

    @Test
    void draft_invalidCategoryCode_throws() {
        assertThatThrownBy(() -> RetentionRule.draft(T, "BAD CODE", null,
                Duration.ofDays(30), "legal basis", null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_periodBelowMinimum_throws() {
        assertThatThrownBy(() -> RetentionRule.draft(T, "marketing", null,
                Duration.ofHours(12), "x", null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_periodAboveMaximum_throws() {
        assertThatThrownBy(() -> RetentionRule.draft(T, "marketing", null,
                Duration.ofDays(365L * 200), "x", null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankLegalBasis_throws() {
        assertThatThrownBy(() -> RetentionRule.draft(T, "marketing", null,
                Duration.ofDays(30), "  ", null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editDraft_changesFields() {
        RetentionRule r = draft();
        r.editDraft("Updated label", Duration.ofDays(60),
                "new basis", "https://ref", NOW.plusSeconds(60));
        assertThat(r.getRetentionPeriod()).isEqualTo(Duration.ofDays(60));
        assertThat(r.getLegalBasis()).isEqualTo("new basis");
        assertThat(r.getDataCategoryLabel()).isEqualTo("Updated label");
    }

    @Test
    void editDraft_whenActive_rejected() {
        RetentionRule r = draft();
        r.activate(NOW);
        assertThatThrownBy(() -> r.editDraft(null,
                Duration.ofDays(60), "x", null, NOW.plusSeconds(60)))
                .isInstanceOf(RetentionRuleStateException.class);
    }

    @Test
    void activate_fromDraft_movesToActive() {
        RetentionRule r = draft();
        r.activate(NOW);
        assertThat(r.isActive()).isTrue();
        assertThat(r.getEffectiveFrom()).isEqualTo(NOW);
    }

    @Test
    void activate_alreadyActive_rejected() {
        RetentionRule r = draft();
        r.activate(NOW);
        assertThatThrownBy(() -> r.activate(NOW.plusSeconds(60)))
                .isInstanceOf(RetentionRuleStateException.class);
    }

    @Test
    void archive_fromActive_movesToArchived() {
        RetentionRule r = draft();
        r.activate(NOW);
        r.archive(NOW.plusSeconds(3600));
        assertThat(r.isArchived()).isTrue();
        assertThat(r.getEffectiveTo()).isEqualTo(NOW.plusSeconds(3600));
    }

    @Test
    void archive_fromDraft_rejected() {
        RetentionRule r = draft();
        assertThatThrownBy(() -> r.archive(NOW))
                .isInstanceOf(RetentionRuleStateException.class);
    }

    @Test
    void computeErasureAt_addsPeriod() {
        RetentionRule r = draft();
        Instant created = NOW;
        assertThat(r.computeErasureAt(created)).isEqualTo(created.plus(Duration.ofDays(30)));
    }

    @Test
    void computeErasureAt_nullRecord_throws() {
        RetentionRule r = draft();
        assertThatThrownBy(() -> r.computeErasureAt(null))
                .isInstanceOf(NullPointerException.class);
    }

    private RetentionRule draft() {
        return RetentionRule.draft(T, "marketing", "Marketing data",
                Duration.ofDays(30), "Consent (Art. 6.1.a)", null, U, NOW);
    }
}
