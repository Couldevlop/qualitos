package com.openlab.qualitos.quality.aiactfria.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FriaTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID V = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant LATER = NOW.plusSeconds(3600);

    @Test
    void draft_initialStateIsDraft() {
        Fria f = draft();
        assertThat(f.isDraft()).isTrue();
        assertThat(f.getStatus()).isEqualTo(FriaStatus.DRAFT);
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> Fria.draft(T, "lowercase", SYS,
                "process", null, "categories", "risks",
                null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankProcess_throws() {
        assertThatThrownBy(() -> Fria.draft(T, "REF-1", SYS,
                " ", null, "cat", "risks",
                null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankCategories_throws() {
        assertThatThrownBy(() -> Fria.draft(T, "REF-1", SYS,
                "process", null, " ", "risks",
                null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankRisks_throws() {
        assertThatThrownBy(() -> Fria.draft(T, "REF-1", SYS,
                "process", null, "cat", " ",
                null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editDraft_changesFields() {
        Fria f = draft();
        f.editDraft("p2", "duration", "cat2", "risks2",
                "mitigation", "oversight", "complaint", LATER);
        assertThat(f.getProcessDescription()).isEqualTo("p2");
        assertThat(f.getMitigationMeasures()).isEqualTo("mitigation");
        assertThat(f.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    void editDraft_afterSubmit_throws() {
        Fria f = approvable();
        f.submit(U, NOW);
        assertThatThrownBy(() -> f.editDraft("p", null, "c", "r",
                "m", "o", "complaint", LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void submit_ok() {
        Fria f = approvable();
        f.submit(U, LATER);
        assertThat(f.getStatus()).isEqualTo(FriaStatus.SUBMITTED);
        assertThat(f.getSubmittedAt()).isEqualTo(LATER);
        assertThat(f.getSubmittedByUserId()).isEqualTo(U);
    }

    @Test
    void submit_missingMitigation_throws() {
        Fria f = draft();
        assertThatThrownBy(() -> f.submit(U, LATER))
                .isInstanceOf(FriaStateException.class)
                .hasMessageContaining("mitigationMeasures");
    }

    @Test
    void submit_missingOversight_throws() {
        Fria f = Fria.draft(T, "REF-1", SYS, "process", null, "cat", "risks",
                "mitigation only", null, null, U, NOW);
        assertThatThrownBy(() -> f.submit(U, LATER))
                .isInstanceOf(FriaStateException.class)
                .hasMessageContaining("humanOversightMeasures");
    }

    @Test
    void submit_nullSubmitter_throws() {
        Fria f = approvable();
        assertThatThrownBy(() -> f.submit(null, LATER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void submit_twice_throws() {
        Fria f = approvable();
        f.submit(U, LATER);
        assertThatThrownBy(() -> f.submit(U, LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void approve_ok() {
        Fria f = approvable();
        f.submit(U, LATER);
        f.approve(V, "looks good", LATER);
        assertThat(f.isApproved()).isTrue();
        assertThat(f.getApprovedByUserId()).isEqualTo(V);
        assertThat(f.getApprovalNotes()).isEqualTo("looks good");
    }

    @Test
    void approve_sameUserAsSubmitter_throws() {
        Fria f = approvable();
        f.submit(U, LATER);
        assertThatThrownBy(() -> f.approve(U, "self", LATER))
                .isInstanceOf(FriaStateException.class)
                .hasMessageContaining("segregation");
    }

    @Test
    void approve_nullApprover_throws() {
        Fria f = approvable();
        f.submit(U, LATER);
        assertThatThrownBy(() -> f.approve(null, "x", LATER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void approve_fromDraft_throws() {
        Fria f = approvable();
        assertThatThrownBy(() -> f.approve(V, null, LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void returnToDraft_resetsStatus() {
        Fria f = approvable();
        f.submit(U, LATER);
        f.returnToDraft("needs more detail", LATER);
        assertThat(f.isDraft()).isTrue();
        assertThat(f.getApprovalNotes()).isEqualTo("needs more detail");
    }

    @Test
    void returnToDraft_fromDraft_throws() {
        Fria f = approvable();
        assertThatThrownBy(() -> f.returnToDraft("x", LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void returnToDraft_blankReason_throws() {
        Fria f = approvable();
        f.submit(U, LATER);
        assertThatThrownBy(() -> f.returnToDraft(" ", LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void archive_fromApproved_ok() {
        Fria f = approvable();
        f.submit(U, LATER);
        f.approve(V, null, LATER);
        Instant end = LATER.plusSeconds(3600);
        f.archive("decommissioned", end);
        assertThat(f.isArchived()).isTrue();
        assertThat(f.getEffectiveTo()).isEqualTo(end);
        assertThat(f.getArchivedReason()).isEqualTo("decommissioned");
    }

    @Test
    void archive_fromDraft_throws() {
        Fria f = approvable();
        assertThatThrownBy(() -> f.archive("r", LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void archive_blankReason_throws() {
        Fria f = approvable();
        f.submit(U, LATER);
        f.approve(V, null, LATER);
        assertThatThrownBy(() -> f.archive(" ", LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void archived_terminalState() {
        Fria f = approvable();
        f.submit(U, LATER);
        f.approve(V, null, LATER);
        f.archive("r", LATER);
        assertThatThrownBy(() -> f.archive("again", LATER))
                .isInstanceOf(FriaStateException.class);
    }

    @Test
    void assignId_setsIdentifier() {
        Fria f = draft();
        UUID id = UUID.randomUUID();
        f.assignId(id);
        assertThat(f.getId()).isEqualTo(id);
    }

    @Test
    void constructor_nullAiSystem_throws() {
        assertThatThrownBy(() -> Fria.draft(T, "REF-1", null,
                "process", null, "cat", "risks", null, null, null, U, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullTenant_throws() {
        assertThatThrownBy(() -> Fria.draft(null, "REF-1", SYS,
                "process", null, "cat", "risks", null, null, null, U, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    private static Fria draft() {
        return Fria.draft(T, "REF-1", SYS,
                "process", null, "categories", "risks",
                null, null, null, U, NOW);
    }

    private static Fria approvable() {
        return Fria.draft(T, "REF-1", SYS,
                "process", "1 year", "categories", "risks",
                "mitigation plan", "human review", "complaint hotline", U, NOW);
    }
}
