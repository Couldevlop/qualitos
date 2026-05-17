package com.openlab.qualitos.quality.aiqms.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiQmsTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID V = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");
    static final Instant LATER = NOW.plusSeconds(86400);

    @Test
    void draft_initial() {
        AiQms q = ready();
        assertThat(q.isDraft()).isTrue();
        assertThat(q.getVersion()).isEqualTo("1.0");
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> AiQms.draft(T, "lowercase", "1.0", "name", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidVersion_throws() {
        assertThatThrownBy(() -> AiQms.draft(T, "REF-1", "v1", "name", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankName_throws() {
        assertThatThrownBy(() -> AiQms.draft(T, "REF-1", "1.0", " ", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_ok() {
        AiQms q = ready();
        q.editDraft("New name", "desc2", "rcs", "dc", "qc", "dm", "rm",
                "pmm", "comm", "rmgmt", "supplier", Set.of(SYS), LATER);
        assertThat(q.getName()).isEqualTo("New name");
        assertThat(q.getCoveredAiSystemIds()).containsExactly(SYS);
    }

    @Test
    void edit_afterApproval_throws() {
        AiQms q = ready();
        q.approve(U, V, "ok", NOW);
        assertThatThrownBy(() -> q.editDraft("x", null, "x", "x", "x", "x", "x",
                "x", "x", null, null, null, LATER))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void approve_ok() {
        AiQms q = ready();
        q.approve(U, V, "ok", LATER);
        assertThat(q.getStatus()).isEqualTo(AiQmsStatus.APPROVED);
        assertThat(q.getSubmittedByUserId()).isEqualTo(U);
        assertThat(q.getApprovedByUserId()).isEqualTo(V);
    }

    @Test
    void approve_sameUser_throws() {
        AiQms q = ready();
        assertThatThrownBy(() -> q.approve(U, U, null, LATER))
                .isInstanceOf(AiQmsStateException.class)
                .hasMessageContaining("segregation");
    }

    @Test
    void approve_missingDescription_throws() {
        AiQms q = AiQms.draft(T, "REF-1", "1.0", "n", null,
                null, "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW);
        assertThatThrownBy(() -> q.approve(U, V, null, LATER))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void approve_nullActors_throws() {
        AiQms q = ready();
        assertThatThrownBy(() -> q.approve(null, V, null, LATER))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> q.approve(U, null, null, LATER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void putInForce_ok() {
        AiQms q = ready();
        q.approve(U, V, "ok", LATER);
        q.putInForce(LATER);
        assertThat(q.isInForce()).isTrue();
        assertThat(q.getEffectiveFrom()).isEqualTo(LATER);
    }

    @Test
    void putInForce_fromDraft_throws() {
        AiQms q = ready();
        assertThatThrownBy(() -> q.putInForce(LATER))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void supersede_ok() {
        AiQms q = ready();
        q.approve(U, V, "ok", LATER);
        q.putInForce(LATER);
        UUID newId = UUID.randomUUID();
        q.supersede(newId, LATER);
        assertThat(q.isSuperseded()).isTrue();
        assertThat(q.getSupersededByQmsId()).isEqualTo(newId);
    }

    @Test
    void supersede_selfReference_throws() {
        AiQms q = ready();
        UUID id = UUID.randomUUID();
        q.assignId(id);
        q.approve(U, V, "ok", LATER);
        q.putInForce(LATER);
        assertThatThrownBy(() -> q.supersede(id, LATER))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void supersede_nullSuccessor_throws() {
        AiQms q = ready();
        q.approve(U, V, "ok", LATER);
        q.putInForce(LATER);
        assertThatThrownBy(() -> q.supersede(null, LATER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void supersede_fromDraft_throws() {
        AiQms q = ready();
        assertThatThrownBy(() -> q.supersede(UUID.randomUUID(), LATER))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void archive_fromDraft_ok() {
        AiQms q = ready();
        q.archive("never used", LATER);
        assertThat(q.isArchived()).isTrue();
        assertThat(q.getArchivedReason()).isEqualTo("never used");
    }

    @Test
    void archive_fromApproved_ok() {
        AiQms q = ready();
        q.approve(U, V, "ok", LATER);
        q.archive("changed strategy", LATER);
        assertThat(q.isArchived()).isTrue();
    }

    @Test
    void archive_fromInForce_ok() {
        AiQms q = ready();
        q.approve(U, V, "ok", LATER);
        q.putInForce(LATER);
        q.archive("retired", LATER);
        assertThat(q.isArchived()).isTrue();
    }

    @Test
    void archive_blankReason_throws() {
        AiQms q = ready();
        assertThatThrownBy(() -> q.archive(" ", LATER))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void supersededTerminal_cannotTransition() {
        AiQms q = ready();
        q.approve(U, V, "ok", LATER);
        q.putInForce(LATER);
        q.supersede(UUID.randomUUID(), LATER);
        assertThatThrownBy(() -> q.archive("x", LATER))
                .isInstanceOf(AiQmsStateException.class);
    }

    @Test
    void sanitizeIds_nullBecomesEmpty() {
        AiQms q = AiQms.draft(T, "REF-1", "1.0", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, null, U, NOW);
        assertThat(q.getCoveredAiSystemIds()).isEmpty();
    }

    @Test
    void assignId() {
        AiQms q = ready();
        UUID id = UUID.randomUUID();
        q.assignId(id);
        assertThat(q.getId()).isEqualTo(id);
    }

    @Test
    void versionWithPatch_ok() {
        AiQms q = AiQms.draft(T, "REF-1", "2.1.3", "n", null,
                "x", "x", "x", "x", "x", "x", "x", null, null, Set.of(), U, NOW);
        assertThat(q.getVersion()).isEqualTo("2.1.3");
    }

    private static AiQms ready() {
        return AiQms.draft(T, "REF-1", "1.0", "Name", "desc",
                "compliance", "design", "quality", "data", "risk",
                "pmm", "comm", "resource", "supplier", Set.of(), U, NOW);
    }
}
