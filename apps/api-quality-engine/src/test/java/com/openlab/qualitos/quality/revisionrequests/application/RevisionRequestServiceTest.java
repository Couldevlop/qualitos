package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestNotFoundException;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestRepository;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStatus;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * L'enregistrement, la décision et leur trace.
 */
class RevisionRequestServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER_TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();
    static final UUID REQUEST = UUID.randomUUID();
    static final UUID TRIGGER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    RevisionRequestRepository repo;
    RevisionApplier applier;
    RevisionAuditPort audit;
    TenantProvider tenants;
    ActorProvider actors;
    RevisionRequestService service;

    @BeforeEach
    void setUp() {
        repo = mock(RevisionRequestRepository.class);
        applier = mock(RevisionApplier.class);
        audit = mock(RevisionAuditPort.class);
        tenants = mock(TenantProvider.class);
        actors = mock(ActorProvider.class);
        when(tenants.requireTenantId()).thenReturn(TENANT);
        when(actors.currentUserId()).thenReturn(USER);
        when(repo.save(any())).thenAnswer(inv -> {
            RevisionRequest request = inv.getArgument(0);
            if (request.getId() == null) request.assignId(UUID.randomUUID());
            return request;
        });
        service = new RevisionRequestService(repo, applier, audit, tenants, actors,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aSecondIdenticalProposalSupersedesThePreviousPendingOne() {
        // Idempotence : une seule demande PENDING par cible. La précédente passe
        // SUPERSEDED, elle ne disparaît pas — l'historique des propositions est
        // lui-même une preuve.
        RevisionRequest previous = pending();
        when(repo.findPendingForTarget(TENANT, RevisionTargetType.PFMEA_ITEM, ITEM))
                .thenReturn(Optional.of(previous));

        service.record(List.of(pending()));

        assertThat(previous.getStatus()).isEqualTo(RevisionRequestStatus.SUPERSEDED);
        verify(repo, times(2)).save(any());
    }

    @Test
    void aCreationProposalNeverSupersedesAnythingBecauseItHasNoTarget() {
        service.record(List.of(RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM_CREATE, null, RevisionTriggerType.NC_CREATED,
                TRIGGER, "NC-1", "aucun mode ne correspond",
                ProposedChange.creation("{}"), NOW)));

        verify(repo, never()).findPendingForTarget(any(), any(), any());
        verify(repo).save(any());
    }

    @Test
    void anEmptyProposalListWritesNothing() {
        service.record(List.of());

        verifyNoInteractions(repo);
    }

    @Test
    void acceptanceAppliesTheChangeThenWritesAnAuditEventWhoseActorComesFromTheToken() {
        // H2 de l'audit du 6 juin : l'acteur d'un fait d'audit ne vient jamais du
        // corps de la requête.
        RevisionRequest request = pending();
        when(repo.findById(REQUEST)).thenReturn(Optional.of(request));

        service.accept(REQUEST);

        verify(applier).apply(request);
        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.ACCEPTED);
        assertThat(request.getDecidedBy()).isEqualTo(USER);
        verify(audit).record(eq(TENANT), eq(USER), eq("revision.request.accepted"),
                any(), any(), contains("\"status\":\"ACCEPTED\""));
    }

    @Test
    void rejectionAlsoWritesAnAuditEvent() {
        // Un refus est une décision qualité. Ne pas le tracer laisserait croire
        // que la proposition n'a jamais existé.
        RevisionRequest request = pending();
        when(repo.findById(REQUEST)).thenReturn(Optional.of(request));

        service.reject(REQUEST, "Cotation revue le 12/08 en revue de risque");

        assertThat(request.getStatus()).isEqualTo(RevisionRequestStatus.REJECTED);
        verify(applier, never()).apply(any());
        verify(audit).record(eq(TENANT), eq(USER), eq("revision.request.rejected"),
                any(), any(), contains("\"status\":\"REJECTED\""));
    }

    @Test
    void acceptingARequestOfAnotherTenantIsNotFound() {
        RevisionRequest foreign = RevisionRequest.propose(OTHER_TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM, ITEM, RevisionTriggerType.NC_CREATED,
                TRIGGER, "NC-1", "justifié", ProposedChange.rating("occurrence", 4, 6), NOW);
        when(repo.findById(REQUEST)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.accept(REQUEST))
                .isInstanceOf(RevisionRequestNotFoundException.class);
        verifyNoInteractions(applier, audit);
    }

    @Test
    void anUnknownRequestIsNotFound() {
        when(repo.findById(REQUEST)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(REQUEST, "non"))
                .isInstanceOf(RevisionRequestNotFoundException.class);
    }

    @Test
    void theReadsAllGoThroughTheTenantOfTheContext() {
        when(repo.findPendingByProduct(TENANT, PRODUCT)).thenReturn(List.of(pending()));
        when(repo.countPendingByProduct(TENANT, PRODUCT)).thenReturn(3);
        when(repo.findByTrigger(TENANT, TRIGGER)).thenReturn(List.of(pending()));

        assertThat(service.pendingForProduct(PRODUCT)).singleElement()
                .extracting(RevisionRequestDto.View::rationale)
                .isEqualTo("3 NC en 12 mois — occurrence 4 → 6");
        assertThat(service.countPending(PRODUCT)).isEqualTo(3);
        assertThat(service.forTrigger(TRIGGER)).hasSize(1);
    }

    @Test
    void theViewExposesTheProposedChangeSoItCanBeContested() {
        when(repo.findPendingByProduct(TENANT, PRODUCT)).thenReturn(List.of(pending()));

        RevisionRequestDto.View view = service.pendingForProduct(PRODUCT).get(0);

        assertThat(view.field()).isEqualTo("occurrence");
        assertThat(view.from()).isEqualTo("4");
        assertThat(view.to()).isEqualTo("6");
        assertThat(view.status()).isEqualTo(RevisionRequestStatus.PENDING);
    }

    private RevisionRequest pending() {
        return RevisionRequest.propose(TENANT, PRODUCT, RevisionTargetType.PFMEA_ITEM, ITEM,
                RevisionTriggerType.NC_CREATED, TRIGGER, "NC-2026-0143",
                "3 NC en 12 mois — occurrence 4 → 6",
                ProposedChange.rating("occurrence", 4, 6), NOW);
    }
}
