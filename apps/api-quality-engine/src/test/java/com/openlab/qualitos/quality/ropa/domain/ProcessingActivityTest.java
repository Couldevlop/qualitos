package com.openlab.qualitos.quality.ropa.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ProcessingActivityTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void draft_validInputs_createsDraft() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null, Set.of(), null);
        assertThat(a.isDraft()).isTrue();
        assertThat(a.getEffectiveFrom()).isNull();
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> ProcessingActivity.draft(T, "lowercase-bad", "n", "p",
                LawfulBasis.CONTRACT, null,
                "Ctrl", "ctrl@x", null, null, null,
                Set.of(), Set.of(), false, null,
                Set.of(), Set.of(), null, Set.of(), null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidCategoryCode_throws() {
        assertThatThrownBy(() -> ProcessingActivity.draft(T, "ROPA-2026-001", "n", "p",
                LawfulBasis.CONTRACT, null,
                "Ctrl", "ctrl@x", null, null, null,
                Set.of("BAD CAT"), Set.of(),
                false, null, Set.of(), Set.of(), null,
                Set.of(), null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidCountryCode_throws() {
        assertThatThrownBy(() -> draft(LawfulBasis.CONTRACT, null, false, null,
                Set.of("us-bad"), "SCC"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_legitimateInterests_withoutLIA_throws() {
        assertThatThrownBy(() -> draft(LawfulBasis.LEGITIMATE_INTERESTS, null, false, null,
                Set.of(), null))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void draft_legitimateInterests_withLIA_ok() {
        ProcessingActivity a = draft(LawfulBasis.LEGITIMATE_INTERESTS,
                "LIA: balancing test favors processing — see /docs/lia/REF-001.pdf",
                false, null, Set.of(), null);
        assertThat(a.getLawfulBasis()).isEqualTo(LawfulBasis.LEGITIMATE_INTERESTS);
    }

    @Test
    void draft_specialCategoriesProcessed_withoutJustification_throws() {
        assertThatThrownBy(() -> draft(LawfulBasis.CONTRACT, null,
                true, null, Set.of(), null))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void draft_specialCategoriesProcessed_withJustification_ok() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null,
                true, "Art. 9§2.h — médecine du travail", Set.of(), null);
        assertThat(a.isSpecialCategoriesProcessed()).isTrue();
    }

    @Test
    void draft_thirdCountryTransfer_withoutSafeguards_throws() {
        assertThatThrownBy(() -> draft(LawfulBasis.CONTRACT, null, false, null,
                Set.of("US"), null))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void draft_thirdCountryTransfer_withSafeguards_ok() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null,
                Set.of("US", "IN"), "SCC 2021 + supplementary measures");
        assertThat(a.getThirdCountryTransfers()).contains("US", "IN");
    }

    @Test
    void editDraft_changesFields() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null, Set.of(), null);
        a.editDraft("Updated", "New purposes", LawfulBasis.CONSENT, null,
                "Ctrl", "ctrl@x", null, null, null,
                Set.of("customers"), Set.of("identity"), false, null,
                Set.of("staff"), Set.of(), null, Set.of(), null, null, NOW.plusSeconds(60));
        assertThat(a.getName()).isEqualTo("Updated");
        assertThat(a.getLawfulBasis()).isEqualTo(LawfulBasis.CONSENT);
        assertThat(a.getDataSubjectCategories()).contains("customers");
    }

    @Test
    void editDraft_whenActive_rejected() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null, Set.of(), null);
        a.activate(NOW);
        assertThatThrownBy(() -> a.editDraft("X", "p", LawfulBasis.CONTRACT, null,
                "C", "c@x", null, null, null,
                Set.of(), Set.of(), false, null,
                Set.of(), Set.of(), null, Set.of(), null, null, NOW.plusSeconds(60)))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void activate_fromDraft_moves() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null, Set.of(), null);
        a.activate(NOW);
        assertThat(a.isActive()).isTrue();
        assertThat(a.getEffectiveFrom()).isEqualTo(NOW);
    }

    @Test
    void activate_twice_rejected() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null, Set.of(), null);
        a.activate(NOW);
        assertThatThrownBy(() -> a.activate(NOW.plusSeconds(60)))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    @Test
    void archive_fromActive_moves() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null, Set.of(), null);
        a.activate(NOW);
        a.archive(NOW.plusSeconds(86400));
        assertThat(a.isArchived()).isTrue();
        assertThat(a.getEffectiveTo()).isEqualTo(NOW.plusSeconds(86400));
    }

    @Test
    void archive_fromDraft_rejected() {
        ProcessingActivity a = draft(LawfulBasis.CONTRACT, null, false, null, Set.of(), null);
        assertThatThrownBy(() -> a.archive(NOW))
                .isInstanceOf(ProcessingActivityStateException.class);
    }

    private ProcessingActivity draft(LawfulBasis basis, String basisDetails,
                                     boolean special, String specialJustif,
                                     Set<String> countries, String safeguards) {
        return ProcessingActivity.draft(T, "ROPA-2026-001", "Customer CRM",
                "Manage customer relationship and orders",
                basis, basisDetails,
                "Acme Corp", "dpo@acme.com", null, null, null,
                Set.of("customers"), Set.of("identity", "contact"),
                special, specialJustif,
                Set.of("internal-staff"), countries, safeguards,
                Set.of(), null, null, U, NOW);
    }
}
