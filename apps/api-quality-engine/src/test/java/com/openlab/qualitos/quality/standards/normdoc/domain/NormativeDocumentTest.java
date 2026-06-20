package com.openlab.qualitos.quality.standards.normdoc.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormativeDocumentTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final UUID SUBMITTER = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-20T10:00:00Z");

    private NormativeDocument draft() {
        return NormativeDocument.draftFromAi(TENANT, STD, "iso-9001", NormDocKind.MANUAL,
                "Manuel Qualité — ACME (iso-9001)",
                List.of(new NormDocSection("ctx", "Contexte", List.of("4.1"), "Corps"),
                        new NormDocSection("lead", "Leadership", List.of(), "Corps 2")),
                "ollama", AUTHOR, NOW);
    }

    @Test
    void draftFromAi_startsInBrouillonIa() {
        NormativeDocument d = draft();
        assertThat(d.getStatus()).isEqualTo(NormDocStatus.BROUILLON_IA);
        assertThat(d.isDraft()).isTrue();
        assertThat(d.getTenantId()).isEqualTo(TENANT);
        assertThat(d.getStandardCode()).isEqualTo("iso-9001");
        assertThat(d.getAiProvider()).isEqualTo("ollama");
        assertThat(d.getCreatedByUserId()).isEqualTo(AUTHOR);
        assertThat(d.getSections()).hasSize(2);
    }

    @Test
    void constructor_rejectsNulls() {
        assertThatThrownBy(() -> new NormativeDocument(null, null, STD, "c", NormDocKind.MANUAL,
                "t", List.of(new NormDocSection("k", "t", List.of(), "b")),
                NormDocStatus.BROUILLON_IA, "p", null, null, null, null, null, null, null,
                AUTHOR, NOW, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejectsBlankCodeAndTitle() {
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, " ", NormDocKind.MANUAL,
                "t", List.of(new NormDocSection("k", "t", List.of(), "b")), "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, "c", NormDocKind.MANUAL,
                " ", List.of(new NormDocSection("k", "t", List.of(), "b")), "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsTooLongTitle() {
        String big = "x".repeat(501);
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, "c", NormDocKind.MANUAL,
                big, List.of(new NormDocSection("k", "t", List.of(), "b")), "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void constructor_rejectsEmptyAndDuplicateAndNullSections() {
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, "c", NormDocKind.MANUAL,
                "t", List.of(), "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, "c", NormDocKind.MANUAL,
                "t", List.of(new NormDocSection("dup", "a", List.of(), "b"),
                        new NormDocSection("dup", "c", List.of(), "d")), "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unique");
        List<NormDocSection> withNull = new java.util.ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, "c", NormDocKind.MANUAL,
                "t", withNull, "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("null section");
    }

    @Test
    void editDraft_onlyInBrouillon() {
        NormativeDocument d = draft();
        d.editDraft("Nouveau titre",
                List.of(new NormDocSection("s", "Section", List.of("5.1"), "Edit")),
                NOW.plusSeconds(10));
        assertThat(d.getTitle()).isEqualTo("Nouveau titre");
        assertThat(d.getSections()).hasSize(1);
        assertThat(d.getUpdatedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void editDraft_rejectedOutsideBrouillon() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        assertThatThrownBy(() -> d.editDraft("x",
                List.of(new NormDocSection("s", "t", List.of(), "b")), NOW))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("BROUILLON_IA");
    }

    @Test
    void submitForReview_movesToEnValidation() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW.plusSeconds(5));
        assertThat(d.getStatus()).isEqualTo(NormDocStatus.EN_VALIDATION);
        assertThat(d.isInReview()).isTrue();
        assertThat(d.getSubmittedByUserId()).isEqualTo(SUBMITTER);
        assertThat(d.getSubmittedAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void submitForReview_rejectsNullSubmitter() {
        NormativeDocument d = draft();
        assertThatThrownBy(() -> d.submitForReview(null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void submit_fromBrouillonAfterReject_clearsRejection() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        d.reject("Manque la portée", NOW.plusSeconds(1));
        assertThat(d.getRejectionReason()).isEqualTo("Manque la portée");
        d.submitForReview(SUBMITTER, NOW.plusSeconds(2));
        assertThat(d.getRejectionReason()).isNull();
        assertThat(d.getStatus()).isEqualTo(NormDocStatus.EN_VALIDATION);
    }

    @Test
    void approve_signsAndIsTerminal() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        d.approve(APPROVER, "sig-abc", "RAS", NOW.plusSeconds(20));
        assertThat(d.getStatus()).isEqualTo(NormDocStatus.APPROUVE);
        assertThat(d.isApproved()).isTrue();
        assertThat(d.getApprovedByUserId()).isEqualTo(APPROVER);
        assertThat(d.getHumanSignature()).isEqualTo("sig-abc");
        assertThat(d.getApprovalNotes()).isEqualTo("RAS");
        // état terminal : aucune transition.
        assertThatThrownBy(() -> d.submitForReview(SUBMITTER, NOW))
                .isInstanceOf(NormDocStateException.class);
    }

    @Test
    void approve_requiresInReview() {
        NormativeDocument d = draft();
        assertThatThrownBy(() -> d.approve(APPROVER, "sig", null, NOW))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void approve_requiresSignature() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        assertThatThrownBy(() -> d.approve(APPROVER, " ", null, NOW))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void approve_rejectsNullApprover() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        assertThatThrownBy(() -> d.approve(null, "sig", null, NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void approve_enforcesSegregationOfDuties() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        assertThatThrownBy(() -> d.approve(SUBMITTER, "sig", null, NOW))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("segregation");
    }

    @Test
    void reject_requiresReasonAndReturnsToBrouillon() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        d.reject("Sections incomplètes", NOW.plusSeconds(3));
        assertThat(d.getStatus()).isEqualTo(NormDocStatus.BROUILLON_IA);
        assertThat(d.getRejectionReason()).isEqualTo("Sections incomplètes");
        assertThat(d.getSubmittedByUserId()).isNull();
        assertThat(d.getSubmittedAt()).isNull();
    }

    @Test
    void reject_requiresNonBlankReason() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        assertThatThrownBy(() -> d.reject(" ", NOW))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void reject_requiresInReview() {
        NormativeDocument d = draft();
        assertThatThrownBy(() -> d.reject("x", NOW))
                .isInstanceOf(NormDocStateException.class);
    }

    @Test
    void toMarkdown_rendersTitleSectionsAndClauses() {
        NormativeDocument d = draft();
        String md = d.toMarkdown();
        assertThat(md).startsWith("# Manuel Qualité — ACME (iso-9001)");
        assertThat(md).contains("## Contexte");
        assertThat(md).contains("*Clauses : 4.1*");
        assertThat(md).contains("## Leadership");
        assertThat(md).doesNotContain("*Clauses : *");
        assertThat(md).endsWith("\n");
    }

    @Test
    void assignId_setsId() {
        NormativeDocument d = draft();
        UUID id = UUID.randomUUID();
        d.assignId(id);
        assertThat(d.getId()).isEqualTo(id);
    }

    @Test
    void approve_nullSignatureRejected() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        assertThatThrownBy(() -> d.approve(APPROVER, null, null, NOW))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void reject_nullReasonRejected() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW);
        assertThatThrownBy(() -> d.reject(null, NOW))
                .isInstanceOf(NormDocStateException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void constructor_nullTitleRejected() {
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, "c", NormDocKind.MANUAL,
                null, List.of(new NormDocSection("k", "t", List.of(), "b")), "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
    }

    @Test
    void constructor_nullSectionsRejected() {
        assertThatThrownBy(() -> NormativeDocument.draftFromAi(TENANT, STD, "c", NormDocKind.MANUAL,
                "t", null, "p", AUTHOR, NOW))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one");
    }

    @Test
    void statusPredicates_falseBranches() {
        NormativeDocument d = draft();
        d.submitForReview(SUBMITTER, NOW); // EN_VALIDATION
        assertThat(d.isDraft()).isFalse();
        assertThat(d.isApproved()).isFalse();
        assertThat(d.isInReview()).isTrue();
        d.approve(APPROVER, "sig", null, NOW); // APPROUVE
        assertThat(d.isInReview()).isFalse();
        assertThat(d.isDraft()).isFalse();
    }

    @Test
    void constructor_defaultsUpdatedAtToCreatedAt() {
        NormativeDocument d = new NormativeDocument(null, TENANT, STD, "c", NormDocKind.POLICY,
                "t", List.of(new NormDocSection("k", "t", List.of(), "b")),
                NormDocStatus.BROUILLON_IA, "p", null, null, null, null, null, null, null,
                AUTHOR, NOW, null);
        assertThat(d.getUpdatedAt()).isEqualTo(NOW);
    }
}
