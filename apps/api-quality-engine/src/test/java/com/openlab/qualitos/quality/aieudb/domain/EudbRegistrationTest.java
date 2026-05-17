package com.openlab.qualitos.quality.aieudb.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EudbRegistrationTest {

    static final UUID T = UUID.randomUUID();
    static final UUID U = UUID.randomUUID();
    static final UUID SYS = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-17T10:00:00Z");
    static final Instant LATER = NOW.plusSeconds(86400);
    static final Instant REG_DATE = NOW.plusSeconds(7L * 86400);

    @Test
    void draft_initial() {
        EudbRegistration r = ready();
        assertThat(r.isDraft()).isTrue();
        assertThat(r.getStatus()).isEqualTo(EudbRegistrationStatus.DRAFT);
    }

    @Test
    void draft_invalidReference_throws() {
        assertThatThrownBy(() -> EudbRegistration.draft(T, "lowercase", SYS,
                "Acme", null, "FR", "purpose", "tech-doc", U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draft_invalidMemberState_throws() {
        assertThatThrownBy(() -> EudbRegistration.draft(T, "REF-1", SYS,
                "Acme", null, "France", "purpose", null, U, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void edit_planned_ok() {
        EudbRegistration r = ready();
        r.editDraft("Acme 2", "EU Rep", "DE", "new purpose", "doc-2", LATER);
        assertThat(r.getProviderEntityName()).isEqualTo("Acme 2");
        assertThat(r.getMemberStateOfReference()).isEqualTo("DE");
    }

    @Test
    void edit_afterSubmit_throws() {
        EudbRegistration r = ready();
        r.submit(U, LATER);
        assertThatThrownBy(() -> r.editDraft("x", null, "FR", "p", null, LATER))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void submit_ok() {
        EudbRegistration r = ready();
        r.submit(U, LATER);
        assertThat(r.getStatus()).isEqualTo(EudbRegistrationStatus.SUBMITTED);
        assertThat(r.getSubmittedByUserId()).isEqualTo(U);
    }

    @Test
    void submit_missingProvider_throws() {
        EudbRegistration r = EudbRegistration.draft(T, "REF-1", SYS,
                null, null, "FR", "purpose", null, U, NOW);
        assertThatThrownBy(() -> r.submit(U, LATER))
                .isInstanceOf(EudbRegistrationStateException.class)
                .hasMessageContaining("providerEntityName");
    }

    @Test
    void submit_missingMemberState_throws() {
        EudbRegistration r = EudbRegistration.draft(T, "REF-1", SYS,
                "Acme", null, null, "purpose", null, U, NOW);
        assertThatThrownBy(() -> r.submit(U, LATER))
                .isInstanceOf(EudbRegistrationStateException.class)
                .hasMessageContaining("memberStateOfReference");
    }

    @Test
    void submit_missingPurpose_throws() {
        EudbRegistration r = EudbRegistration.draft(T, "REF-1", SYS,
                "Acme", null, "FR", null, null, U, NOW);
        assertThatThrownBy(() -> r.submit(U, LATER))
                .isInstanceOf(EudbRegistrationStateException.class)
                .hasMessageContaining("intendedPurposeSummary");
    }

    @Test
    void submit_nullSubmitter_throws() {
        EudbRegistration r = ready();
        assertThatThrownBy(() -> r.submit(null, LATER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void markRegistered_ok() {
        EudbRegistration r = ready();
        r.submit(U, LATER);
        r.markRegistered("EUDB-AI-ABC123", REG_DATE, REG_DATE);
        assertThat(r.isRegistered()).isTrue();
        assertThat(r.getEudbId()).isEqualTo("EUDB-AI-ABC123");
        assertThat(r.getRegistrationDate()).isEqualTo(REG_DATE);
    }

    @Test
    void markRegistered_invalidEudbId_throws() {
        EudbRegistration r = ready();
        r.submit(U, LATER);
        assertThatThrownBy(() -> r.markRegistered("invalid", REG_DATE, REG_DATE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markRegistered_nullDate_throws() {
        EudbRegistration r = ready();
        r.submit(U, LATER);
        assertThatThrownBy(() -> r.markRegistered("EUDB-AI-ABC123", null, REG_DATE))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void markRegistered_fromDraft_throws() {
        EudbRegistration r = ready();
        assertThatThrownBy(() -> r.markRegistered("EUDB-AI-ABC123", REG_DATE, REG_DATE))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void declareUpdate_ok() {
        EudbRegistration r = registered();
        Instant upd = REG_DATE.plusSeconds(86400);
        r.declareUpdate("model retrained", upd, upd);
        assertThat(r.isUpdated()).isTrue();
        assertThat(r.getLastUpdateSummary()).isEqualTo("model retrained");
    }

    @Test
    void declareUpdate_chainedUpdates() {
        EudbRegistration r = registered();
        Instant u1 = REG_DATE.plusSeconds(86400);
        Instant u2 = u1.plusSeconds(86400);
        r.declareUpdate("v1.1", u1, u1);
        r.declareUpdate("v1.2", u2, u2);
        assertThat(r.getLastUpdateDate()).isEqualTo(u2);
    }

    @Test
    void declareUpdate_beforeRegistration_throws() {
        EudbRegistration r = registered();
        assertThatThrownBy(() -> r.declareUpdate("x", REG_DATE.minusSeconds(1), REG_DATE))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void declareUpdate_blankSummary_throws() {
        EudbRegistration r = registered();
        assertThatThrownBy(() -> r.declareUpdate(" ", REG_DATE.plusSeconds(1), REG_DATE))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void declareUpdate_beforePreviousUpdate_throws() {
        EudbRegistration r = registered();
        Instant u1 = REG_DATE.plusSeconds(86400);
        r.declareUpdate("v1.1", u1, u1);
        assertThatThrownBy(() -> r.declareUpdate("v0.9", REG_DATE.plusSeconds(1), REG_DATE))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void reject_fromDraft_ok() {
        EudbRegistration r = ready();
        r.reject("incomplete", LATER);
        assertThat(r.getStatus()).isEqualTo(EudbRegistrationStatus.REJECTED);
    }

    @Test
    void reject_fromSubmitted_ok() {
        EudbRegistration r = ready();
        r.submit(U, LATER);
        r.reject("clarification needed", LATER);
        assertThat(r.getStatus()).isEqualTo(EudbRegistrationStatus.REJECTED);
    }

    @Test
    void reject_blankReason_throws() {
        EudbRegistration r = ready();
        assertThatThrownBy(() -> r.reject(" ", LATER))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void retire_fromRegistered_ok() {
        EudbRegistration r = registered();
        r.retire("end of life", LATER);
        assertThat(r.getStatus()).isEqualTo(EudbRegistrationStatus.RETIRED);
    }

    @Test
    void retire_fromDraft_throws() {
        EudbRegistration r = ready();
        assertThatThrownBy(() -> r.retire("x", LATER))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void retire_blankReason_throws() {
        EudbRegistration r = registered();
        assertThatThrownBy(() -> r.retire(" ", LATER))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void terminalStates_noTransition() {
        EudbRegistration r = ready();
        r.reject("x", LATER);
        assertThatThrownBy(() -> r.submit(U, LATER))
                .isInstanceOf(EudbRegistrationStateException.class);
    }

    @Test
    void isActive_trueForRegisteredAndUpdated() {
        EudbRegistration r = registered();
        assertThat(r.isActive()).isTrue();
        r.declareUpdate("upd", REG_DATE.plusSeconds(1), REG_DATE);
        assertThat(r.isActive()).isTrue();
    }

    @Test
    void assignId() {
        EudbRegistration r = ready();
        UUID id = UUID.randomUUID();
        r.assignId(id);
        assertThat(r.getId()).isEqualTo(id);
    }

    private static EudbRegistration ready() {
        return EudbRegistration.draft(T, "REF-1", SYS,
                "Acme Corp", "EU Rep SARL", "FR",
                "purpose summary", "TECH-DOC-1", U, NOW);
    }

    private static EudbRegistration registered() {
        EudbRegistration r = ready();
        r.submit(U, LATER);
        r.markRegistered("EUDB-AI-ABC123", REG_DATE, REG_DATE);
        return r;
    }
}
