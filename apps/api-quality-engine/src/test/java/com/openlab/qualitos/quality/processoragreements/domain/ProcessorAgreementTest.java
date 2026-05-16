package com.openlab.qualitos.quality.processoragreements.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ProcessorAgreementTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void draft_validInputs_createsDraft() {
        ProcessorAgreement a = draft();
        assertThat(a.isDraft()).isTrue();
        assertThat(a.getBreachNotificationCommitmentHours()).isEqualTo(72);
        assertThat(a.getProcessorCountry()).isEqualTo("US");
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "lowercase",
                "Acme", null, null, null, null, "Service",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72,
                false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidCountry_throws() {
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, "USA", "Service",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72,
                false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_breachHoursOutOfRange_throws() {
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, null, "Service",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 0,
                false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, null, "Service",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 721,
                false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_thirdCountryWithoutSafeguards_throws() {
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, "US", "Service",
                Set.of(), Set.of(), Set.of("US"), null, null,
                null, null, null, null, 72,
                false, null, null, U, NOW))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void draft_thirdCountryWithSafeguards_ok() {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, "US", "Service",
                Set.of(), Set.of(), Set.of("US", "IN"), "SCC 2021", null,
                null, null, null, null, 72,
                false, null, null, U, NOW);
        assertThat(a.getThirdCountryTransfers()).contains("US", "IN");
    }

    @Test
    void editDraft_changesFields() {
        ProcessorAgreement a = draft();
        a.editDraft("Updated", null, "ops@acme.com", null,
                "US", "Updated services",
                Set.of("cloud"), Set.of(), Set.of(), null, null,
                NOW, NOW, NOW.plusSeconds(86400),
                "AES-256", 24, true, "right to audit", "delete in 30d",
                NOW.plusSeconds(60));
        assertThat(a.getProcessorName()).isEqualTo("Updated");
        assertThat(a.isAuditRights()).isTrue();
        assertThat(a.getBreachNotificationCommitmentHours()).isEqualTo(24);
    }

    @Test
    void editDraft_whenActive_rejected() {
        ProcessorAgreement a = readyToActivate();
        a.activate(NOW);
        assertThatThrownBy(() -> a.editDraft("X", null, "ops@x", null, null,
                "S", Set.of(), Set.of(), Set.of(), null, null,
                NOW, NOW, null, null, 72, false, null, null,
                NOW.plusSeconds(60)))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void activate_withoutSignedAt_throws() {
        ProcessorAgreement a = draft();
        assertThatThrownBy(() -> a.activate(NOW))
                .isInstanceOf(ProcessorAgreementStateException.class)
                .hasMessageContaining("signedAt");
    }

    @Test
    void activate_withoutEffectiveFrom_throws() {
        ProcessorAgreement a = draft();
        a.editDraft(a.getProcessorName(), null, "contact@x", null, null,
                a.getServicesDescription(), Set.of(), Set.of(), Set.of(),
                null, null, NOW, null, null, null, 72, false, null, null, NOW);
        assertThatThrownBy(() -> a.activate(NOW))
                .isInstanceOf(ProcessorAgreementStateException.class)
                .hasMessageContaining("effectiveFrom");
    }

    @Test
    void activate_withoutProcessorContact_throws() {
        ProcessorAgreement a = draft();
        a.editDraft(a.getProcessorName(), null, null, null, null,
                a.getServicesDescription(), Set.of(), Set.of(), Set.of(),
                null, null, NOW, NOW, null, null, 72, false, null, null, NOW);
        assertThatThrownBy(() -> a.activate(NOW))
                .isInstanceOf(ProcessorAgreementStateException.class)
                .hasMessageContaining("processorContact");
    }

    @Test
    void activate_succeedsWhenAllPresent() {
        ProcessorAgreement a = readyToActivate();
        a.activate(NOW.plusSeconds(60));
        assertThat(a.isActive()).isTrue();
    }

    @Test
    void terminate_requiresReason() {
        ProcessorAgreement a = readyToActivate();
        a.activate(NOW);
        assertThatThrownBy(() -> a.terminate(" ", NOW.plusSeconds(60)))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void terminate_fromActive_succeeds() {
        ProcessorAgreement a = readyToActivate();
        a.activate(NOW);
        a.terminate("end of services", NOW.plusSeconds(60));
        assertThat(a.getStatus()).isEqualTo(ProcessorAgreementStatus.TERMINATED);
        assertThat(a.getTerminatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void terminate_fromDraft_rejected() {
        ProcessorAgreement a = draft();
        assertThatThrownBy(() -> a.terminate("x", NOW))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    @Test
    void expireIfDue_pastDate_marksExpired() {
        ProcessorAgreement a = withExpiration(NOW.plusSeconds(86400));
        a.activate(NOW);
        a.expireIfDue(NOW.plusSeconds(86400 * 2));
        assertThat(a.getStatus()).isEqualTo(ProcessorAgreementStatus.EXPIRED);
    }

    @Test
    void expireIfDue_beforeExpiry_noop() {
        ProcessorAgreement a = withExpiration(NOW.plusSeconds(86400));
        a.activate(NOW);
        a.expireIfDue(NOW.plusSeconds(60));
        assertThat(a.getStatus()).isEqualTo(ProcessorAgreementStatus.ACTIVE);
    }

    @Test
    void expireIfDue_alreadyTerminated_noop() {
        ProcessorAgreement a = withExpiration(NOW.plusSeconds(86400));
        a.activate(NOW);
        a.terminate("done", NOW.plusSeconds(60));
        a.expireIfDue(NOW.plusSeconds(86400 * 2));
        assertThat(a.getStatus()).isEqualTo(ProcessorAgreementStatus.TERMINATED);
    }

    @Test
    void isExpirable_trueWhenActivePastDate() {
        ProcessorAgreement a = withExpiration(NOW.plusSeconds(86400));
        a.activate(NOW);
        assertThat(a.isExpirable(NOW.plusSeconds(86400 * 2))).isTrue();
        assertThat(a.isExpirable(NOW)).isFalse();
    }

    @Test
    void draft_nullCountry_treatedAsNull() {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, null, "S",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, U, NOW);
        assertThat(a.getProcessorCountry()).isNull();
    }

    @Test
    void draft_blankCountry_treatedAsNull() {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, "  ", "S",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, U, NOW);
        assertThat(a.getProcessorCountry()).isNull();
    }

    @Test
    void draft_invalidSubProcessorCategoryCode_throws() {
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, null, "S",
                Set.of("BAD CODE"), Set.of(), Set.of(),
                null, null, null, null, null, null, 72,
                false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidThirdCountryCode_throws() {
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, null, "S",
                Set.of(), Set.of(), Set.of("BAD"), "SCC",
                null, null, null, null, null, 72,
                false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_nullSetsAndNulls_handled() {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-NULLS",
                "Acme", null, null, null, null, "S",
                null, null, null, null, null,
                null, null, null, null, 72, false, null, null, U, NOW);
        assertThat(a.getSubProcessorCategories()).isEmpty();
        assertThat(a.getLinkedProcessingActivityIds()).isEmpty();
        assertThat(a.getThirdCountryTransfers()).isEmpty();
    }

    @Test
    void draft_referenceTooLong_throws() {
        String tooLong = "A".repeat(65);
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, tooLong,
                "Acme", null, null, null, null, "S",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_blankServicesDescription_throws() {
        assertThatThrownBy(() -> ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, null, null, null, "  ",
                Set.of(), Set.of(), Set.of(), null, null,
                null, null, null, null, 72, false, null, null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void editDraft_removingSafeguardsWhileHavingTransfer_throws() {
        ProcessorAgreement a = ProcessorAgreement.draft(T, "DPA-1",
                "Acme", null, "ops@x", null, null, "S",
                Set.of(), Set.of(), Set.of("US"), "SCC",
                null, NOW, NOW, null, null, 72, false, null, null, U, NOW);
        assertThatThrownBy(() -> a.editDraft("Acme", null, "ops@x", null, null,
                "S", Set.of(), Set.of(), Set.of("US"), null,
                null, NOW, NOW, null, null, 72, false, null, null, NOW))
                .isInstanceOf(ProcessorAgreementStateException.class);
    }

    private ProcessorAgreement draft() {
        return ProcessorAgreement.draft(T, "DPA-2026-001",
                "Acme Corp", "Acme Corp Ltd", null, null, "us",
                "Cloud hosting services", Set.of(), Set.of(), Set.of(),
                null, null, null, null, null,
                null, 72, false, null, null, U, NOW);
    }

    private ProcessorAgreement readyToActivate() {
        return ProcessorAgreement.draft(T, "DPA-2026-001",
                "Acme Corp", null, "ops@acme.com", null, "US",
                "Cloud hosting services", Set.of(), Set.of(), Set.of(),
                null, null, NOW, NOW, null, null,
                72, false, null, null, U, NOW);
    }

    private ProcessorAgreement withExpiration(Instant exp) {
        return ProcessorAgreement.draft(T, "DPA-2026-001",
                "Acme Corp", null, "ops@acme.com", null, "US",
                "Cloud hosting services", Set.of(), Set.of(), Set.of(),
                null, null, NOW, NOW, exp, null,
                72, false, null, null, U, NOW);
    }
}
