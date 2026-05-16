package com.openlab.qualitos.quality.dpia.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class DpiaTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID DPO = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void draft_validInputs_createsDraft() {
        Dpia d = draft(RiskLevel.LOW);
        assertThat(d.isDraft()).isTrue();
        assertThat(d.getOverallRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(d.isConsultationRequired()).isFalse();
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> Dpia.draft(T, "lowercase-bad", "t", null,
                Set.of(), RiskLevel.LOW, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editDraft_changesFields() {
        Dpia d = draft(RiskLevel.LOW);
        d.editDraft("Updated", "desc", Set.of(UUID.randomUUID()),
                "necessity", "risks", "mitigations",
                RiskLevel.MEDIUM, false, null, NOW.plusSeconds(60));
        assertThat(d.getTitle()).isEqualTo("Updated");
        assertThat(d.getOverallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void editDraft_whenNotDraft_rejected() {
        Dpia d = draft(RiskLevel.LOW);
        d.start(U, NOW);
        assertThatThrownBy(() -> d.editDraft("X", null, Set.of(),
                "n", "r", "m", RiskLevel.LOW, false, null, NOW.plusSeconds(60)))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void start_fromDraft_moves() {
        Dpia d = draft(RiskLevel.LOW);
        d.start(U, NOW);
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.IN_PROGRESS);
        assertThat(d.getHandledByUserId()).isEqualTo(U);
    }

    @Test
    void returnToDraft_fromInProgress_moves() {
        Dpia d = draft(RiskLevel.LOW);
        d.start(U, NOW);
        d.returnToDraft(NOW.plusSeconds(60));
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.DRAFT);
    }

    @Test
    void returnToDraft_notInProgress_rejected() {
        Dpia d = draft(RiskLevel.LOW);
        assertThatThrownBy(() -> d.returnToDraft(NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void submitToDpo_requiresAnalysisFields() {
        Dpia d = draft(RiskLevel.LOW);
        d.start(U, NOW);
        assertThatThrownBy(() -> d.submitToDpo(NOW.plusSeconds(60)))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void submitToDpo_inProgressWithAnalysis_moves() {
        Dpia d = draft(RiskLevel.LOW);
        // Édite en DRAFT avec les champs requis, puis démarre puis soumet.
        d.editDraft(d.getTitle(), d.getDescription(),
                d.getLinkedProcessingActivityIds(),
                "necessity ok", "risks identified", "mitigations applied",
                RiskLevel.LOW, false, null, NOW.plusSeconds(60));
        d.start(U, NOW.plusSeconds(120));
        d.submitToDpo(NOW.plusSeconds(180));
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.DPO_REVIEW);
    }

    @Test
    void approve_lowRisk_succeeds() {
        Dpia d = readyForReview(RiskLevel.LOW);
        d.approve(DPO, "Conforme — analyse adéquate", NOW.plusSeconds(300));
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.APPROVED);
        assertThat(d.getDpoUserId()).isEqualTo(DPO);
        assertThat(d.getEffectiveFrom()).isEqualTo(NOW.plusSeconds(300));
    }

    @Test
    void approve_highRisk_withoutConsultation_rejected() {
        Dpia d = readyForReview(RiskLevel.HIGH);
        assertThatThrownBy(() -> d.approve(DPO, "x", NOW.plusSeconds(300)))
                .isInstanceOf(DpiaStateException.class)
                .hasMessageContaining("Art. 36");
    }

    @Test
    void approve_highRisk_withConsultation_succeeds() {
        Dpia d = draft(RiskLevel.HIGH);
        d.editDraft(d.getTitle(), d.getDescription(),
                d.getLinkedProcessingActivityIds(),
                "necessity", "risks", "mitigations",
                RiskLevel.HIGH, true, "CNIL prior consultation initiated", NOW);
        d.start(U, NOW.plusSeconds(60));
        d.submitToDpo(NOW.plusSeconds(120));
        d.approve(DPO, "Approuvée sous réserve consultation CNIL", NOW.plusSeconds(180));
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.APPROVED);
    }

    @Test
    void approve_missingDpoUserId_throws() {
        Dpia d = readyForReview(RiskLevel.LOW);
        assertThatThrownBy(() -> d.approve(null, "x", NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void approve_blankOpinion_throws() {
        Dpia d = readyForReview(RiskLevel.LOW);
        assertThatThrownBy(() -> d.approve(DPO, " ", NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void reject_succeeds() {
        Dpia d = readyForReview(RiskLevel.MEDIUM);
        d.reject(DPO, "Non conforme — refonte requise", NOW.plusSeconds(300));
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.REJECTED);
        assertThat(d.isTerminal()).isTrue();
    }

    @Test
    void archive_fromApproved_succeeds() {
        Dpia d = readyForReview(RiskLevel.LOW);
        d.approve(DPO, "ok", NOW);
        d.archive(NOW.plusSeconds(86400));
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.ARCHIVED);
    }

    @Test
    void archive_fromDraft_rejected() {
        Dpia d = draft(RiskLevel.LOW);
        assertThatThrownBy(() -> d.archive(NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void draft_nullReference_throws() {
        assertThatThrownBy(() -> Dpia.draft(T, null, "t", null,
                Set.of(), RiskLevel.LOW, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_referenceTooLong_throws() {
        String tooLong = "A".repeat(65);
        assertThatThrownBy(() -> Dpia.draft(T, tooLong, "t", null,
                Set.of(), RiskLevel.LOW, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankTitle_throws() {
        assertThatThrownBy(() -> Dpia.draft(T, "DPIA-1", " ", null,
                Set.of(), RiskLevel.LOW, U, NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void draft_nullLinkedSet_treatedAsEmpty() {
        Dpia d = Dpia.draft(T, "DPIA-1", "t", null,
                null, RiskLevel.LOW, U, NOW);
        assertThat(d.getLinkedProcessingActivityIds()).isEmpty();
    }

    @Test
    void submitToDpo_inDraft_rejected() {
        Dpia d = draftDefault(RiskLevel.LOW);
        assertThatThrownBy(() -> d.submitToDpo(NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void approve_inDraft_rejected() {
        Dpia d = draftDefault(RiskLevel.LOW);
        assertThatThrownBy(() -> d.approve(DPO, "ok", NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void approve_severeRisk_withoutConsultationNotes_throws() {
        Dpia d = draftDefault(RiskLevel.SEVERE);
        d.editDraft(d.getTitle(), null, Set.of(),
                "necessity", "risks", "mitigations",
                RiskLevel.SEVERE, true, null, NOW);
        d.start(U, NOW);
        d.submitToDpo(NOW);
        assertThatThrownBy(() -> d.approve(DPO, "ok", NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void reject_missingDpoUser_throws() {
        Dpia d = readyForReview(RiskLevel.LOW);
        assertThatThrownBy(() -> d.reject(null, "x", NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void reject_blankOpinion_throws() {
        Dpia d = readyForReview(RiskLevel.LOW);
        assertThatThrownBy(() -> d.reject(DPO, " ", NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void archive_fromRejected_succeeds() {
        Dpia d = readyForReview(RiskLevel.LOW);
        d.reject(DPO, "no", NOW);
        d.archive(NOW.plusSeconds(86400));
        assertThat(d.getStatus()).isEqualTo(DpiaStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_rejected() {
        Dpia d = readyForReview(RiskLevel.LOW);
        d.approve(DPO, "ok", NOW);
        d.archive(NOW.plusSeconds(60));
        assertThatThrownBy(() -> d.archive(NOW.plusSeconds(120)))
                .isInstanceOf(DpiaStateException.class);
    }

    @Test
    void start_alreadyStarted_rejected() {
        Dpia d = readyForReview(RiskLevel.LOW);
        assertThatThrownBy(() -> d.start(U, NOW))
                .isInstanceOf(DpiaStateException.class);
    }

    private Dpia draftDefault(RiskLevel initial) {
        return draft(initial);
    }

    private Dpia draft(RiskLevel initial) {
        return Dpia.draft(T, "DPIA-2026-001", "Hiring background check",
                "Pre-employment screening",
                Set.of(UUID.randomUUID()), initial, U, NOW);
    }

    private Dpia withAnalysis(RiskLevel level) {
        Dpia d = draft(level);
        d.editDraft(d.getTitle(), d.getDescription(),
                d.getLinkedProcessingActivityIds(),
                "necessity ok", "risks identified", "mitigations applied",
                level, false, null, NOW);
        return d;
    }

    private Dpia readyForReview(RiskLevel level) {
        Dpia d = withAnalysis(level);
        d.start(U, NOW.plusSeconds(60));
        d.submitToDpo(NOW.plusSeconds(120));
        return d;
    }
}
