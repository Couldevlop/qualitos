package com.openlab.qualitos.quality.crossbordertransfers.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CrossBorderTransferTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void draft_validInputs_createsDraft() {
        CrossBorderTransfer t = draftSCC();
        assertThat(t.isDraft()).isTrue();
        assertThat(t.getMechanism()).isEqualTo(TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES);
        assertThat(t.getDestinationCountries()).contains("US");
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> CrossBorderTransfer.draft(T, "lowercase",
                "Acme", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021", null, null, Set.of(), Set.of(), Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidCountry_throws() {
        assertThatThrownBy(() -> CrossBorderTransfer.draft(T, "CBT-1",
                "Acme", null, null, Set.of("USA"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidDataCategory_throws() {
        assertThatThrownBy(() -> CrossBorderTransfer.draft(T, "CBT-1",
                "Acme", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of("BAD CAT"), Set.of(), Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activate_withoutCountries_throws() {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-1",
                "Acme", null, null, Set.of() /* no countries */,
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), U, NOW);
        assertThatThrownBy(() -> t.activate(NOW))
                .isInstanceOf(CrossBorderTransferStateException.class)
                .hasMessageContaining("destinationCountries");
    }

    @Test
    void activate_withoutSafeguards_throws() {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-1",
                "Acme", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                null /* no safeguards */, null, null, Set.of(), Set.of(), Set.of(), U, NOW);
        assertThatThrownBy(() -> t.activate(NOW))
                .isInstanceOf(CrossBorderTransferStateException.class)
                .hasMessageContaining("safeguardsDescription");
    }

    @Test
    void activate_derogationWithoutJustification_throws() {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-1",
                "Acme", null, null, Set.of("US"),
                TransferMechanism.DEROGATION_ART49,
                "Art. 49.1.b", null, null /* no justification */,
                Set.of(), Set.of(), Set.of(), U, NOW);
        assertThatThrownBy(() -> t.activate(NOW))
                .isInstanceOf(CrossBorderTransferStateException.class)
                .hasMessageContaining("Art. 49");
    }

    @Test
    void activate_succeeds_withAllRequired() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW.plusSeconds(60));
        assertThat(t.isActive()).isTrue();
        assertThat(t.getEffectiveFrom()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void suspend_thenReactivate_clearsSuspension() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        t.suspend("Audit en cours", NOW.plusSeconds(60));
        assertThat(t.isSuspended()).isTrue();
        assertThat(t.getSuspensionReason()).isEqualTo("Audit en cours");

        t.activate(NOW.plusSeconds(120));
        assertThat(t.isActive()).isTrue();
        assertThat(t.getSuspendedAt()).isNull();
        assertThat(t.getSuspensionReason()).isNull();
    }

    @Test
    void suspend_requiresReason() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        assertThatThrownBy(() -> t.suspend(" ", NOW))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void suspend_fromDraft_rejected() {
        CrossBorderTransfer t = draftSCC();
        assertThatThrownBy(() -> t.suspend("r", NOW))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void terminate_fromActive_succeeds() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        t.terminate("Fin contrat", NOW.plusSeconds(86400));
        assertThat(t.isTerminal()).isTrue();
        assertThat(t.getEffectiveTo()).isEqualTo(NOW.plusSeconds(86400));
    }

    @Test
    void terminate_fromSuspended_succeeds() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        t.suspend("Pause", NOW);
        t.terminate("Fin contrat", NOW.plusSeconds(60));
        assertThat(t.getStatus()).isEqualTo(CrossBorderTransferStatus.TERMINATED);
    }

    @Test
    void terminate_requiresReason() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        assertThatThrownBy(() -> t.terminate(null, NOW))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void terminate_alreadyTerminated_rejected() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        t.terminate("r", NOW);
        assertThatThrownBy(() -> t.terminate("r2", NOW.plusSeconds(60)))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void editDraft_changesFields() {
        CrossBorderTransfer t = draftSCC();
        t.editDraft("Updated", null, null,
                Set.of("CA", "IN"), TransferMechanism.ADEQUACY_DECISION,
                "Adequacy CA 2021", null, null,
                Set.of("customer-pii"), Set.of(), Set.of(), NOW.plusSeconds(60));
        assertThat(t.getRecipientName()).isEqualTo("Updated");
        assertThat(t.getMechanism()).isEqualTo(TransferMechanism.ADEQUACY_DECISION);
        assertThat(t.getDestinationCountries()).contains("CA", "IN");
    }

    @Test
    void editDraft_whenActive_rejected() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        assertThatThrownBy(() -> t.editDraft("X", null, null,
                Set.of("US"), TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), NOW.plusSeconds(60)))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void draft_nullSets_handled() {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-1",
                "Acme", null, null, null,
                TransferMechanism.ADEQUACY_DECISION,
                "Adequacy", null, null, null, null, null, U, NOW);
        assertThat(t.getDestinationCountries()).isEmpty();
        assertThat(t.getDataCategories()).isEmpty();
        assertThat(t.getLinkedProcessingActivityIds()).isEmpty();
        assertThat(t.getLinkedProcessorAgreementIds()).isEmpty();
    }

    @Test
    void draft_blankRecipientName_throws() {
        assertThatThrownBy(() -> CrossBorderTransfer.draft(T, "CBT-1",
                " ", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_referenceTooLong_throws() {
        String tooLong = "A".repeat(65);
        assertThatThrownBy(() -> CrossBorderTransfer.draft(T, tooLong,
                "Acme", null, null, Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC", null, null, Set.of(), Set.of(), Set.of(), U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void activate_alreadyActive_rejected() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        assertThatThrownBy(() -> t.activate(NOW.plusSeconds(60)))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void suspend_alreadySuspended_rejected() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        t.suspend("r", NOW);
        assertThatThrownBy(() -> t.suspend("r2", NOW.plusSeconds(60)))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void activate_alreadyTerminated_rejected() {
        CrossBorderTransfer t = draftSCC();
        t.activate(NOW);
        t.terminate("r", NOW);
        assertThatThrownBy(() -> t.activate(NOW.plusSeconds(60)))
                .isInstanceOf(CrossBorderTransferStateException.class);
    }

    @Test
    void derogation_withJustification_activatesOk() {
        CrossBorderTransfer t = CrossBorderTransfer.draft(T, "CBT-DR",
                "Acme", null, null, Set.of("US"),
                TransferMechanism.DEROGATION_ART49,
                "Art. 49.1.b — execution of contract", null,
                "Justification : exécution d'un contrat à la demande du sujet (Art. 49.1.b)",
                Set.of(), Set.of(), Set.of(), U, NOW);
        t.activate(NOW);
        assertThat(t.isActive()).isTrue();
    }

    private CrossBorderTransfer draftSCC() {
        return CrossBorderTransfer.draft(T, "CBT-2026-001",
                "Acme Cloud Inc", "Acme Cloud Inc", "ops@acme.com",
                Set.of("US"),
                TransferMechanism.STANDARD_CONTRACTUAL_CLAUSES,
                "SCC 2021 + supplementary measures (TIA performed)",
                "https://example.com/scc.pdf", null,
                Set.of("identity", "contact"),
                Set.of(UUID.randomUUID()), Set.of(UUID.randomUUID()),
                U, NOW);
    }
}
