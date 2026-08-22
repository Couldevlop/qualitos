package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.capa.CapaTransition;
import com.openlab.qualitos.quality.capa.CapaTransitionEvent;
import com.openlab.qualitos.quality.nonconformity.NcCreatedEvent;
import com.openlab.qualitos.quality.revisionrequests.application.CapaRevisionTrigger;
import com.openlab.qualitos.quality.revisionrequests.application.NcRevisionTrigger;
import com.openlab.qualitos.quality.revisionrequests.application.RevisionRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Une proposition de révision est un confort. La non-conformité est le fait.
 * Si le moteur tombe, la NC doit rester enregistrée : c'est la seule hiérarchie
 * acceptable entre les deux.
 */
class RevisionRequestEventListenersTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");
    static final NcCreatedEvent EVENT =
            new NcCreatedEvent(TENANT, NC, PRODUCT, null, "Bavure", null, NOW);

    NcRevisionTrigger ncTrigger;
    CapaRevisionTrigger capaTrigger;
    RevisionRequestService service;
    RevisionRequestEventListeners listeners;

    @BeforeEach
    void setUp() {
        ncTrigger = mock(NcRevisionTrigger.class);
        capaTrigger = mock(CapaRevisionTrigger.class);
        service = mock(RevisionRequestService.class);
        listeners = new RevisionRequestEventListeners(ncTrigger, capaTrigger, service);
    }

    @Test
    void aFailingTriggerNeverPropagatesToTheCaller() {
        // Le déclencheur lève ; l'écouteur journalise et se tait. Sans cette garde,
        // une erreur du moteur ferait échouer une saisie de défaut au poste.
        when(ncTrigger.propose(EVENT)).thenThrow(new IllegalStateException("moteur cassé"));

        assertThatCode(() -> listeners.onNcCreated(EVENT)).doesNotThrowAnyException();
        verifyNoInteractions(service);
    }

    @Test
    void aFailingRecordNeverPropagatesEither() {
        when(ncTrigger.propose(EVENT)).thenReturn(List.of());
        doThrow(new IllegalStateException("base indisponible")).when(service).record(any());

        assertThatCode(() -> listeners.onNcCreated(EVENT)).doesNotThrowAnyException();
    }

    @Test
    void aWorkingTriggerHandsItsProposalsToTheService() {
        when(ncTrigger.propose(EVENT)).thenReturn(List.of());

        listeners.onNcCreated(EVENT);

        verify(service).record(List.of());
    }

    @Test
    void onlyTheClosedCapaTransitionReachesTheTrigger() {
        CapaTransitionEvent resolved = new CapaTransitionEvent(
                TENANT, CapaTransition.RESOLVED, Map.of("id", UUID.randomUUID().toString()));

        listeners.onCapaTransition(resolved);

        verifyNoInteractions(capaTrigger, service);
    }

    @Test
    void aClosedCapaReachesTheTrigger() {
        CapaTransitionEvent closed = new CapaTransitionEvent(
                TENANT, CapaTransition.CLOSED, Map.of("id", UUID.randomUUID().toString()));
        when(capaTrigger.propose(closed)).thenReturn(List.of());

        listeners.onCapaTransition(closed);

        verify(service).record(List.of());
    }

    @Test
    void aFailingCapaTriggerNeverPropagatesToTheCaller() {
        CapaTransitionEvent closed = new CapaTransitionEvent(
                TENANT, CapaTransition.CLOSED, Map.of("id", UUID.randomUUID().toString()));
        when(capaTrigger.propose(closed)).thenThrow(new IllegalStateException("moteur cassé"));

        assertThatCode(() -> listeners.onCapaTransition(closed)).doesNotThrowAnyException();
    }

    @Test
    void theListenersRunAfterCommitNotBefore() throws Exception {
        // Avant le commit, la NC lue par le moteur pourrait disparaître d'un
        // rollback. La contrainte est portée par l'annotation : la vérifier par
        // réflexion est la seule façon de la verrouiller sans monter un contexte
        // Spring complet.
        assertPhaseIsAfterCommit("onNcCreated", NcCreatedEvent.class);
        assertPhaseIsAfterCommit("onCapaTransition", CapaTransitionEvent.class);
    }

    private void assertPhaseIsAfterCommit(String methodName, Class<?> eventType) throws Exception {
        Method method = RevisionRequestEventListeners.class.getMethod(methodName, eventType);
        TransactionalEventListener annotation = method.getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
