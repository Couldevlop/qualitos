package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.capa.CapaTransition;
import com.openlab.qualitos.quality.capa.CapaTransitionEvent;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Ce que la clôture d'une CAPA sur efficacité vérifiée permet de proposer.
 */
class CapaRevisionTriggerTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PROJECT = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();
    static final UUID CAPA = UUID.randomUUID();
    static final UUID ACTION = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    NcLookupPort ncLookup;
    CapaActionsPort capaActions;
    PfmeaPort pfmea;
    CapaRevisionTrigger trigger;

    @BeforeEach
    void setUp() {
        ncLookup = mock(NcLookupPort.class);
        capaActions = mock(CapaActionsPort.class);
        pfmea = mock(PfmeaPort.class);
        trigger = new CapaRevisionTrigger(ncLookup, capaActions, pfmea,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void onlyTheClosedTransitionTriggersAnything() {
        // CapaTransition.CLOSED signifie « clos APRÈS vérification d'efficacité ».
        // RESOLVED ne suffit pas : rien n'a encore été démontré.
        assertThat(trigger.propose(new CapaTransitionEvent(TENANT, CapaTransition.RESOLVED, payload())))
                .isEmpty();
        verifyNoInteractions(ncLookup, capaActions, pfmea);
    }

    @Test
    void aCorrectiveActionProposesToLowerTheOccurrence() {
        // Supprimer la cause fait baisser l'occurrence.
        givenOrigin(ITEM);
        givenItemRated(6, 5);
        givenActions(action("Reglage de la presse corrige", "cause supprimee", false));

        List<RevisionRequest> proposals = trigger.propose(closed());

        RevisionRequest rating = firstPfmea(proposals);
        assertThat(rating.getChange().field()).isEqualTo("occurrence");
        assertThat(rating.getChange().from()).isEqualTo("6");
        assertThat(rating.getChange().to()).isEqualTo("5");
        assertThat(rating.getTriggerType()).isEqualTo(RevisionTriggerType.CAPA_CLOSED);
        assertThat(rating.getTriggerRefId()).isEqualTo(CAPA);
    }

    @Test
    void anActionThatAddsAControlProposesToLowerTheDetectionRating() {
        // Ajouter un contrôle améliore la détection — et améliorer la détection
        // veut dire BAISSER la note. C'est l'erreur classique : la vérifier ici.
        givenOrigin(ITEM);
        givenItemRated(6, 7);
        givenActions(action("Mise en place d'un controle a 100 %", null, false));

        RevisionRequest rating = firstPfmea(trigger.propose(closed()));

        assertThat(rating.getChange().field()).isEqualTo("detection");
        assertThat(rating.getChange().from()).isEqualTo("7");
        assertThat(rating.getChange().to()).isEqualTo("6");
    }

    @Test
    void anAccentedControlWordIsRecognisedJustTheSame() {
        givenOrigin(ITEM);
        givenItemRated(6, 7);
        givenActions(action("Vérification systématique en fin de ligne", null, false));

        assertThat(firstPfmea(trigger.propose(closed())).getChange().field()).isEqualTo("detection");
    }

    @Test
    void aContainmentActionProposesNothing() {
        // Une mesure d'endiguement est temporaire par définition. La graver dans
        // un control plan serait un contresens : on lève l'endiguement quand la
        // cause est traitée.
        givenOrigin(ITEM);
        givenActions(action("Tri a 100 % du lot suspect", null, true));

        assertThat(trigger.propose(closed())).isEmpty();
    }

    @Test
    void eachNonContainmentActionProposesAControlPlanLine() {
        // La ligne est pré-remplie depuis l'intitulé de l'action, en DRAFT de
        // proposition — elle n'entre au plan que si un humain l'accepte.
        givenOrigin(ITEM);
        givenItemRated(6, 5);
        givenActions(action("Reglage de la presse corrige", "detail", false),
                action("Formation des operateurs", null, false));

        List<RevisionRequest> proposals = trigger.propose(closed());

        assertThat(proposals).filteredOn(p ->
                        p.getTargetType() == RevisionTargetType.CONTROL_PLAN_LINE_CREATE)
                .hasSize(2)
                .allSatisfy(p -> assertThat(p.getTargetId()).isNull());
    }

    @Test
    void eachActionSpeaksForItselfAndAlwaysAboutTheSameFailureMode() {
        // Chaque action émet au plus UNE proposition, et toutes visent la même
        // ligne d'analyse. C'est le service qui n'en laissera qu'une en attente :
        // la base n'admet qu'une demande PENDING par cible.
        givenOrigin(ITEM);
        givenItemRated(6, 7);
        givenActions(action("Mise en place d'un controle", null, false),
                action("Reglage corrige", null, false));

        List<RevisionRequest> proposals = trigger.propose(closed());

        assertThat(proposals).filteredOn(p -> p.getTargetType() == RevisionTargetType.PFMEA_ITEM)
                .hasSize(2)
                .extracting(RevisionRequest::getTargetId)
                .containsOnly(ITEM);
    }

    @Test
    void aCapaWhoseSourceIsNotAProductNcProposesNothing() {
        Map<String, Object> payload = payload();
        payload.put("sourceType", "AUDIT");

        assertThat(trigger.propose(new CapaTransitionEvent(TENANT, CapaTransition.CLOSED, payload)))
                .isEmpty();
        verifyNoInteractions(ncLookup);
    }

    @Test
    void aCapaWhoseNcCarriesNoProductProposesNothing() {
        when(ncLookup.findByReference(TENANT, "NC-2026-0143"))
                .thenReturn(Optional.of(new NcLookupPort.NcRef(NC, null, null)));

        assertThat(trigger.propose(closed())).isEmpty();
        verifyNoInteractions(capaActions);
    }

    @Test
    void anUnknownNcReferenceProposesNothing() {
        when(ncLookup.findByReference(TENANT, "NC-2026-0143")).thenReturn(Optional.empty());

        assertThat(trigger.propose(closed())).isEmpty();
    }

    @Test
    void aCapaWithoutASourceReferenceProposesNothing() {
        Map<String, Object> payload = payload();
        payload.remove("sourceRef");

        assertThat(trigger.propose(new CapaTransitionEvent(TENANT, CapaTransition.CLOSED, payload)))
                .isEmpty();
    }

    @Test
    void aPayloadWithAnUnreadableCapaIdProposesNothing() {
        Map<String, Object> payload = payload();
        payload.put("id", "pas-un-uuid");
        givenOrigin(ITEM);

        assertThat(trigger.propose(new CapaTransitionEvent(TENANT, CapaTransition.CLOSED, payload)))
                .isEmpty();
        verifyNoInteractions(capaActions);
    }

    @Test
    void aNcWithoutAFailureModeStillProposesAControlPlanLine() {
        givenOrigin(null);
        givenActions(action("Reglage corrige", null, false));

        List<RevisionRequest> proposals = trigger.propose(closed());

        assertThat(proposals).singleElement()
                .extracting(RevisionRequest::getTargetType)
                .isEqualTo(RevisionTargetType.CONTROL_PLAN_LINE_CREATE);
        verifyNoInteractions(pfmea);
    }

    @Test
    void aRatingAlreadyAtItsFloorIsNotLoweredAgain() {
        givenOrigin(ITEM);
        givenItemRated(1, 1);
        givenActions(action("Reglage corrige", null, false));

        assertThat(trigger.propose(closed()))
                .noneMatch(p -> p.getTargetType() == RevisionTargetType.PFMEA_ITEM);
    }

    // ---------- montage ----------

    private RevisionRequest firstPfmea(List<RevisionRequest> proposals) {
        return proposals.stream()
                .filter(p -> p.getTargetType() == RevisionTargetType.PFMEA_ITEM)
                .findFirst()
                .orElseThrow(() -> new AssertionError("aucune proposition sur le PFMEA"));
    }

    private void givenOrigin(UUID fmeaItemId) {
        when(ncLookup.findByReference(TENANT, "NC-2026-0143"))
                .thenReturn(Optional.of(new NcLookupPort.NcRef(NC, PRODUCT, fmeaItemId)));
    }

    private void givenItemRated(int occurrence, int detection) {
        when(pfmea.item(TENANT, ITEM)).thenReturn(Optional.of(new PfmeaPort.PfmeaItemSnapshot(
                ITEM, PROJECT, PRODUCT, "Bavure sur alésage", 7, occurrence, detection)));
    }

    private void givenActions(CapaActionsPort.CapaActionSummary... actions) {
        when(capaActions.actionsOf(TENANT, CAPA)).thenReturn(List.of(actions));
    }

    private CapaActionsPort.CapaActionSummary action(String title, String description,
                                                     boolean containment) {
        return new CapaActionsPort.CapaActionSummary(
                containment ? UUID.randomUUID() : ACTION, title, description, containment);
    }

    private CapaTransitionEvent closed() {
        return new CapaTransitionEvent(TENANT, CapaTransition.CLOSED, payload());
    }

    private Map<String, Object> payload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", CAPA.toString());
        payload.put("title", "Bavures répétées sur le support moteur");
        payload.put("sourceType", "NON_CONFORMITY");
        payload.put("sourceRef", "NC-2026-0143");
        return payload;
    }
}
