package com.openlab.qualitos.quality.revisionrequests.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RevisionRequestTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID TARGET = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    private RevisionRequest pending() {
        return RevisionRequest.propose(TENANT, PRODUCT, RevisionTargetType.PFMEA_ITEM, TARGET,
                RevisionTriggerType.NC_CREATED, NC, "NC-2026-0143",
                "3 NC en 12 mois sur ce mode de défaillance — occurrence 4 → 6",
                new ProposedChange("occurrence", "4", "6", null), NOW);
    }

    @Test
    void aFreshRequestIsPending() {
        assertThat(pending().getStatus()).isEqualTo(RevisionRequestStatus.PENDING);
    }

    @Test
    void rejectingWithoutANoteIsRefused() {
        // L'auditeur veut lire POURQUOI on n'a pas bougé. Un refus muet est
        // exactement l'écart qu'il cherche.
        assertThatThrownBy(() -> pending().reject(USER, "  ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectingWithANoteRecordsTheDecider() {
        RevisionRequest request = pending();

        request.reject(USER, "Cotation déjà revue le 12/08 en revue de risque", NOW);

        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.REJECTED);
        assertThat(request.getDecidedBy()).isEqualTo(USER);
        assertThat(request.getDecidedAt()).isEqualTo(NOW);
        assertThat(request.getDecisionNote()).startsWith("Cotation déjà revue");
    }

    @Test
    void acceptingRecordsTheDeciderToo() {
        RevisionRequest request = pending();

        request.accept(USER, NOW);

        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.ACCEPTED);
        assertThat(request.getDecidedBy()).isEqualTo(USER);
        assertThat(request.getDecidedAt()).isEqualTo(NOW);
        assertThat(request.getDecisionNote()).isNull();
    }

    @Test
    void aDecidedRequestIsFinalInBothDirections() {
        RevisionRequest accepted = pending();
        accepted.accept(USER, NOW);

        assertThatThrownBy(() -> accepted.reject(USER, "trop tard", NOW))
                .isInstanceOf(RevisionRequestStateException.class);

        RevisionRequest rejected = pending();
        rejected.reject(USER, "non", NOW);

        assertThatThrownBy(() -> rejected.accept(USER, NOW))
                .isInstanceOf(RevisionRequestStateException.class);
    }

    @Test
    void onlyAPendingRequestCanBeSuperseded() {
        RevisionRequest accepted = pending();
        accepted.accept(USER, NOW);

        assertThatThrownBy(() -> accepted.supersede(NOW))
                .isInstanceOf(RevisionRequestStateException.class);
    }

    @Test
    void aSupersededRequestKeepsItsHistoryAndRefusesAnyDecision() {
        RevisionRequest request = pending();

        request.supersede(NOW);

        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.SUPERSEDED);
        assertThat(request.getRationale()).contains("occurrence 4 → 6");
        assertThatThrownBy(() -> request.accept(USER, NOW))
                .isInstanceOf(RevisionRequestStateException.class);
    }

    @Test
    void aCreationRequestHasNoTarget() {
        RevisionRequest request = RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM_CREATE, null,
                RevisionTriggerType.NC_CREATED, NC, "NC-2026-0143",
                "Aucun mode de défaillance ne correspond à cette NC",
                new ProposedChange(null, null, null, "{\"failureMode\":\"Bavure sur alésage\"}"), NOW);

        assertThat(request.getTargetId()).isNull();
        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.PENDING);
        assertThat(request.getTargetType().isCreation()).isTrue();
        assertThat(request.getTargetType().isPfmea()).isTrue();
    }

    @Test
    void aModificationRequestWithoutTargetIsRefused() {
        assertThatThrownBy(() -> RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, null,
                RevisionTriggerType.NC_CREATED, NC, "NC-2026-0143", "…",
                new ProposedChange("occurrence", "4", "6", null), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRequestWithoutARationaleIsRefused() {
        // Une proposition sans justification ne se conteste pas, donc ne se
        // confirme pas en conscience.
        assertThatThrownBy(() -> RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, TARGET,
                RevisionTriggerType.NC_CREATED, NC, "NC-2026-0143", "   ",
                new ProposedChange("occurrence", "4", "6", null), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRequestWithoutATriggerLabelIsRefused() {
        assertThatThrownBy(() -> RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, TARGET,
                RevisionTriggerType.NC_CREATED, NC, " ", "justifié",
                new ProposedChange("occurrence", "4", "6", null), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRehydratedRequestKeepsItsDecisionWithoutReplayingIt() {
        RevisionRequest request = RevisionRequest.rehydrate(TARGET, TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, TARGET, RevisionTriggerType.CAPA_CLOSED, NC,
                "CAPA-1", "justifié", ProposedChange.rating("detection", 6, 5),
                RevisionRequestStatus.REJECTED, USER, NOW, "déjà traité", NOW, NOW);

        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.REJECTED);
        assertThat(request.getDecisionNote()).isEqualTo("déjà traité");
        assertThat(request.getChange().to()).isEqualTo("5");
        assertThat(request.getTriggerType()).isEqualTo(RevisionTriggerType.CAPA_CLOSED);
    }

    @Test
    void aRehydratedRequestWithoutStatusFallsBackToPending() {
        RevisionRequest request = RevisionRequest.rehydrate(TARGET, TENANT, PRODUCT,
                RevisionTargetType.CONTROL_PLAN_LINE_CREATE, null, RevisionTriggerType.CAPA_CLOSED,
                NC, "CAPA-1", "justifié", ProposedChange.creation("{}"), null,
                null, null, null, NOW, NOW);

        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.PENDING);
        assertThat(request.getTargetType().isPfmea()).isFalse();
    }
}
