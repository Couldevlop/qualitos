package com.openlab.qualitos.quality.automateddecisions.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class AutomatedDecisionRecordTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID DPIA = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void draft_validInputs_createsDraft() {
        AutomatedDecisionRecord r = draftProfiling();
        assertThat(r.isDraft()).isTrue();
        assertThat(r.getDecisionType()).isEqualTo(AutomatedDecisionType.PROFILING_ONLY);
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> AutomatedDecisionRecord.draft(T, "lowercase",
                "Name", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankName_throws() {
        assertThatThrownBy(() -> AutomatedDecisionRecord.draft(T, "ADM-1",
                " ", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidInputCategory_throws() {
        assertThatThrownBy(() -> AutomatedDecisionRecord.draft(T, "ADM-1",
                "Name", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of("BAD CAT"), Set.of(), null,
                null, null, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activate_profiling_succeeds() {
        AutomatedDecisionRecord r = draftProfiling();
        r.activate(NOW.plusSeconds(60));
        assertThat(r.isActive()).isTrue();
        assertThat(r.getEffectiveFrom()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void activate_legalEffect_withoutLawfulBasis_throws() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-LE",
                "Credit", null,
                AutomatedDecisionType.AUTOMATED_DECISION_WITH_LEGAL_EFFECT,
                null, null, Set.of(), Set.of(), DPIA,
                "algo", "high", "human review", null, U, NOW);
        assertThatThrownBy(() -> r.activate(NOW))
                .isInstanceOf(AutomatedDecisionStateException.class)
                .hasMessageContaining("Art. 22.2");
    }

    @Test
    void activate_legalEffect_withoutHumanReview_throws() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-LE",
                "Credit", null,
                AutomatedDecisionType.AUTOMATED_DECISION_WITH_LEGAL_EFFECT,
                Art22LawfulBasis.CONTRACTUAL_NECESSITY, null,
                Set.of(), Set.of(), DPIA,
                "algo", "high", null, null, U, NOW);
        assertThatThrownBy(() -> r.activate(NOW))
                .isInstanceOf(AutomatedDecisionStateException.class)
                .hasMessageContaining("Art. 22.3");
    }

    @Test
    void activate_legalEffect_withoutDpia_throws() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-LE",
                "Credit", null,
                AutomatedDecisionType.AUTOMATED_DECISION_WITH_LEGAL_EFFECT,
                Art22LawfulBasis.EXPLICIT_CONSENT, null,
                Set.of(), Set.of(), null /* no DPIA */,
                "algo", "high", "human review", null, U, NOW);
        assertThatThrownBy(() -> r.activate(NOW))
                .isInstanceOf(AutomatedDecisionStateException.class)
                .hasMessageContaining("Art. 35.3.a");
    }

    @Test
    void activate_legalEffect_authorizedByLaw_withoutDetails_throws() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-LE",
                "Credit", null,
                AutomatedDecisionType.AUTOMATED_DECISION_WITH_LEGAL_EFFECT,
                Art22LawfulBasis.AUTHORIZED_BY_LAW, null,
                Set.of(), Set.of(), DPIA,
                "algo", "high", "human review", null, U, NOW);
        assertThatThrownBy(() -> r.activate(NOW))
                .isInstanceOf(AutomatedDecisionStateException.class)
                .hasMessageContaining("citation");
    }

    @Test
    void activate_legalEffect_allFieldsPresent_ok() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-LE",
                "Credit", null,
                AutomatedDecisionType.AUTOMATED_DECISION_WITH_LEGAL_EFFECT,
                Art22LawfulBasis.AUTHORIZED_BY_LAW,
                "Loi n° 2024-XYZ Article 12",
                Set.of(), Set.of(), DPIA,
                "algo", "high", "human review", "opposition", U, NOW);
        r.activate(NOW);
        assertThat(r.isActive()).isTrue();
    }

    @Test
    void editDraft_changesFields() {
        AutomatedDecisionRecord r = draftProfiling();
        r.editDraft("Updated", "new desc",
                AutomatedDecisionType.AUTOMATED_DECISION,
                null, null, Set.of("payment-history"), Set.of(), null,
                "algo", "sig", "human", "obj", NOW.plusSeconds(60));
        assertThat(r.getName()).isEqualTo("Updated");
        assertThat(r.getDecisionType()).isEqualTo(AutomatedDecisionType.AUTOMATED_DECISION);
    }

    @Test
    void editDraft_whenActive_rejected() {
        AutomatedDecisionRecord r = draftProfiling();
        r.activate(NOW);
        assertThatThrownBy(() -> r.editDraft("X", null,
                AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of(), Set.of(), null,
                null, null, null, null, NOW.plusSeconds(60)))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void deprecate_fromActive_succeeds() {
        AutomatedDecisionRecord r = draftProfiling();
        r.activate(NOW);
        r.deprecate(NOW.plusSeconds(60));
        assertThat(r.getStatus()).isEqualTo(AutomatedDecisionStatus.DEPRECATED);
    }

    @Test
    void deprecate_fromDraft_rejected() {
        AutomatedDecisionRecord r = draftProfiling();
        assertThatThrownBy(() -> r.deprecate(NOW))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void archive_fromActive_succeeds() {
        AutomatedDecisionRecord r = draftProfiling();
        r.activate(NOW);
        r.archive(NOW.plusSeconds(86400));
        assertThat(r.isTerminal()).isTrue();
    }

    @Test
    void archive_fromDeprecated_succeeds() {
        AutomatedDecisionRecord r = draftProfiling();
        r.activate(NOW);
        r.deprecate(NOW.plusSeconds(60));
        r.archive(NOW.plusSeconds(120));
        assertThat(r.isTerminal()).isTrue();
    }

    @Test
    void archive_fromDraft_rejected() {
        AutomatedDecisionRecord r = draftProfiling();
        assertThatThrownBy(() -> r.archive(NOW))
                .isInstanceOf(AutomatedDecisionStateException.class);
    }

    @Test
    void nullSetsAndNulls_handled() {
        AutomatedDecisionRecord r = AutomatedDecisionRecord.draft(T, "ADM-NULL",
                "N", null, AutomatedDecisionType.PROFILING_ONLY,
                null, null, null, null, null,
                null, null, null, null, U, NOW);
        assertThat(r.getInputDataCategories()).isEmpty();
        assertThat(r.getLinkedProcessingActivityIds()).isEmpty();
    }

    private AutomatedDecisionRecord draftProfiling() {
        return AutomatedDecisionRecord.draft(T, "ADM-2026-001",
                "Recommandations contenu", null,
                AutomatedDecisionType.PROFILING_ONLY,
                null, null, Set.of("browsing-history"), Set.of(), null,
                null, null, null, "Unsubscribe link", U, NOW);
    }
}
