package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestStateException;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Où atterrit une proposition acceptée — et surtout, où elle n'atterrit jamais.
 */
class RevisionApplierTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PROJECT = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();
    static final UUID TRIGGER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    PfmeaPort pfmea;
    ControlPlanDraftPort controlPlans;
    RevisionApplier applier;

    @BeforeEach
    void setUp() {
        pfmea = mock(PfmeaPort.class);
        controlPlans = mock(ControlPlanDraftPort.class);
        applier = new RevisionApplier(pfmea, controlPlans);
    }

    @Test
    void acceptingOnAnActiveDocumentOpensANewDraftRevision() {
        // Le document en vigueur n'est JAMAIS modifié sous les pieds de la
        // production : la révision suivante naît en brouillon, et l'approbation
        // reste une décision distincte, réservée à un rôle plus étroit.
        givenItem();
        when(pfmea.isProjectActive(TENANT, PROJECT)).thenReturn(true);

        applier.apply(ratingRequest());

        // L'ordre compte : la révision s'ouvre AVANT toute écriture sur la cotation.
        InOrder order = inOrder(pfmea);
        order.verify(pfmea).openRevision(TENANT, PROJECT);
        order.verify(pfmea).updateRating(TENANT, ITEM, "occurrence", 6);
    }

    @Test
    void acceptingWhenADraftRevisionIsAlreadyOpenAddsToIt() {
        // Sinon deux demandes acceptées coup sur coup créeraient deux brouillons
        // concurrents, et l'index partiel d'unicité en rejetterait un.
        givenItem();
        when(pfmea.isProjectActive(TENANT, PROJECT)).thenReturn(false);

        applier.apply(ratingRequest());

        verify(pfmea, never()).openRevision(TENANT, PROJECT);
        verify(pfmea).updateRating(TENANT, ITEM, "occurrence", 6);
    }

    @Test
    void acceptingAFailureModeCreationAddsTheLineToThePfmea() {
        when(pfmea.activeProjectOf(TENANT, PRODUCT)).thenReturn(Optional.of(PROJECT));
        when(pfmea.isProjectActive(TENANT, PROJECT)).thenReturn(true);

        applier.apply(RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM_CREATE, null, RevisionTriggerType.NC_CREATED,
                TRIGGER, "NC-2026-0143", "aucun mode ne correspond",
                ProposedChange.creation(
                        "{\"failureMode\":\"Bavure sur alésage\",\"failureEffect\":\"Montage impossible\"}"),
                NOW));

        verify(pfmea).openRevision(TENANT, PROJECT);
        verify(pfmea).addItem(TENANT, PROJECT, "Bavure sur alésage", "Montage impossible");
    }

    @Test
    void aProductWithoutAnyPfmeaCannotReceiveTheProposal() {
        when(pfmea.activeProjectOf(TENANT, PRODUCT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applier.apply(RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.PFMEA_ITEM_CREATE, null, RevisionTriggerType.NC_CREATED,
                TRIGGER, "NC-2026-0143", "aucun mode ne correspond",
                ProposedChange.creation("{}"), NOW)))
                .isInstanceOf(RevisionRequestStateException.class);
    }

    @Test
    void acceptingAControlPlanLineCreationAddsTheLineToTheDraftPlan() {
        when(controlPlans.draftPlanFor(TENANT, PRODUCT)).thenReturn(Optional.of(PLAN));

        applier.apply(RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.CONTROL_PLAN_LINE_CREATE, null, RevisionTriggerType.CAPA_CLOSED,
                TRIGGER, "CAPA-12", "l'action a produit son effet",
                ProposedChange.creation("{\"characteristicLabel\":\"Couple de serrage\","
                        + "\"controlMethod\":\"Clé dynamométrique\",\"fmeaItemId\":\"" + ITEM + "\"}"),
                NOW));

        verify(controlPlans).addLine(TENANT, PLAN, "Couple de serrage", "Clé dynamométrique", ITEM);
        verifyNoInteractions(pfmea);
    }

    @Test
    void aProductWithoutAnyControlPlanCannotReceiveTheProposal() {
        when(controlPlans.draftPlanFor(TENANT, PRODUCT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applier.apply(RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.CONTROL_PLAN_LINE_CREATE, null, RevisionTriggerType.CAPA_CLOSED,
                TRIGGER, "CAPA-12", "justifié", ProposedChange.creation("{}"), NOW)))
                .isInstanceOf(RevisionRequestStateException.class);
    }

    @Test
    void aDraftWithoutAnyReadableFieldIsAppliedAsAnEmptyLineRatherThanFailing() {
        // Refuser d'appliquer une proposition parce qu'un libellé manque serait
        // disproportionné : la ligne existe, un humain la complètera.
        when(controlPlans.draftPlanFor(TENANT, PRODUCT)).thenReturn(Optional.of(PLAN));

        applier.apply(RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.CONTROL_PLAN_LINE_CREATE, null, RevisionTriggerType.CAPA_CLOSED,
                TRIGGER, "CAPA-12", "justifié", ProposedChange.creation(null), NOW));

        verify(controlPlans).addLine(TENANT, PLAN, "", "", null);
    }

    @Test
    void anEscapedQuoteInTheDraftIsReadBackVerbatim() {
        when(controlPlans.draftPlanFor(TENANT, PRODUCT)).thenReturn(Optional.of(PLAN));

        applier.apply(RevisionRequest.propose(TENANT, PRODUCT,
                RevisionTargetType.CONTROL_PLAN_LINE_CREATE, null, RevisionTriggerType.CAPA_CLOSED,
                TRIGGER, "CAPA-12", "justifié",
                ProposedChange.creation("{\"characteristicLabel\":\"Cote \\\"A\\\"\","
                        + "\"controlMethod\":\"\",\"fmeaItemId\":\"pas-un-uuid\"}"),
                NOW));

        verify(controlPlans).addLine(TENANT, PLAN, "Cote \"A\"", "", null);
    }

    @Test
    void aFailureModeThatVanishedBeforeAcceptanceIsRefusedRatherThanGuessed() {
        when(pfmea.item(TENANT, ITEM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applier.apply(ratingRequest()))
                .isInstanceOf(RevisionRequestStateException.class);
    }

    private void givenItem() {
        when(pfmea.item(TENANT, ITEM)).thenReturn(Optional.of(new PfmeaPort.PfmeaItemSnapshot(
                ITEM, PROJECT, PRODUCT, "Bavure sur alésage", 7, 4, 5)));
    }

    private RevisionRequest ratingRequest() {
        return RevisionRequest.propose(TENANT, PRODUCT, RevisionTargetType.PFMEA_ITEM, ITEM,
                RevisionTriggerType.NC_CREATED, TRIGGER, "NC-2026-0143",
                "3 NC en 12 mois — occurrence 4 → 6",
                ProposedChange.rating("occurrence", 4, 6), NOW);
    }
}
