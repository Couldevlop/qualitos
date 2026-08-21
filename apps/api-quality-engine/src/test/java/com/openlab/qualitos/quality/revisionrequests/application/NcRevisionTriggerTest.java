package com.openlab.qualitos.quality.revisionrequests.application;

import com.openlab.qualitos.quality.nonconformity.NcCreatedEvent;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Ce que le PFMEA devrait dire quand un défaut vient d'être constaté.
 */
class NcRevisionTriggerTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID PROJECT = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    NcHistoryPort history;
    PfmeaPort pfmea;
    NcRevisionTrigger trigger;

    @BeforeEach
    void setUp() {
        history = mock(NcHistoryPort.class);
        pfmea = mock(PfmeaPort.class);
        trigger = new NcRevisionTrigger(history, pfmea, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aNcWithoutProductProposesNothing() {
        // L'événement est publié pour TOUTE NC : le filtre est ici, chez le
        // consommateur, pas chez l'émetteur.
        List<RevisionRequest> proposals = trigger.propose(
                new NcCreatedEvent(TENANT, NC, null, null, "Retard de livraison", null, NOW));

        assertThat(proposals).isEmpty();
        verifyNoInteractions(pfmea, history);
    }

    @Test
    void aNcAttachedToAFailureModeRaisesTheOccurrenceWhenHistoryContradictsIt() {
        // 3 NC sur 12 mois, item coté 4 -> demande d'occurrence 4 -> 6.
        givenItemRated(4, 5);
        when(history.countForProductAndFailureMode(eq(TENANT), eq(PRODUCT), eq(ITEM), any()))
                .thenReturn(3);

        List<RevisionRequest> proposals = trigger.propose(event(ITEM));

        assertThat(proposals).hasSize(1);
        RevisionRequest proposal = proposals.get(0);
        assertThat(proposal.getTargetType()).isEqualTo(RevisionTargetType.PFMEA_ITEM);
        assertThat(proposal.getTargetId()).isEqualTo(ITEM);
        assertThat(proposal.getTriggerType()).isEqualTo(RevisionTriggerType.NC_CREATED);
        assertThat(proposal.getTriggerRefId()).isEqualTo(NC);
        assertThat(proposal.getChange().field()).isEqualTo("occurrence");
        assertThat(proposal.getChange().from()).isEqualTo("4");
        assertThat(proposal.getChange().to()).isEqualTo("6");
    }

    @Test
    void theRationaleNamesTheCountAndTheWindow() {
        // « 3 NC en 12 mois sur ce mode de défaillance » : sans le chiffre et sans
        // la fenêtre, l'utilisateur ne peut ni vérifier ni contester la proposition.
        givenItemRated(4, 5);
        when(history.countForProductAndFailureMode(eq(TENANT), eq(PRODUCT), eq(ITEM), any()))
                .thenReturn(3);

        String rationale = trigger.propose(event(ITEM)).get(0).getRationale();

        assertThat(rationale).contains("3 NC").contains("12 mois").contains("4 → 6");
    }

    @Test
    void theCountingWindowIsTwelveMonthsBeforeNow() {
        givenItemRated(4, 5);
        when(history.countForProductAndFailureMode(eq(TENANT), eq(PRODUCT), eq(ITEM), any()))
                .thenReturn(3);

        trigger.propose(event(ITEM));

        verify(history).countForProductAndFailureMode(TENANT, PRODUCT, ITEM,
                Instant.parse("2025-08-19T08:00:00Z"));
    }

    @Test
    void aNcWithoutFailureModeProposesToCreateOne() {
        // Un mode de défaillance existe dans la vraie vie sans exister dans
        // l'analyse : c'est l'écart le plus intéressant à montrer à un auditeur.
        when(pfmea.activeProjectOf(TENANT, PRODUCT)).thenReturn(Optional.of(PROJECT));

        List<RevisionRequest> proposals = trigger.propose(event(null));

        assertThat(proposals).hasSize(1);
        RevisionRequest proposal = proposals.get(0);
        assertThat(proposal.getTargetType()).isEqualTo(RevisionTargetType.PFMEA_ITEM_CREATE);
        assertThat(proposal.getTargetId()).isNull();
        assertThat(proposal.getChange().draftJson()).contains("Bavure sur alésage");
    }

    @Test
    void aProductWithoutAnActivePfmeaProposesNothingToCreate() {
        when(pfmea.activeProjectOf(TENANT, PRODUCT)).thenReturn(Optional.empty());

        assertThat(trigger.propose(event(null))).isEmpty();
    }

    @Test
    void nothingIsProposedWhenTheRatingAlreadyCoversTheHistory() {
        givenItemRated(8, 5);
        when(history.countForProductAndFailureMode(eq(TENANT), eq(PRODUCT), eq(ITEM), any()))
                .thenReturn(3);

        assertThat(trigger.propose(event(ITEM))).isEmpty();
    }

    @Test
    void aFailureModeThatVanishedFromTheAnalysisProposesNothing() {
        when(pfmea.item(TENANT, ITEM)).thenReturn(Optional.empty());

        assertThat(trigger.propose(event(ITEM))).isEmpty();
        verifyNoInteractions(history);
    }

    @Test
    void theTriggerReadsTheTenantOfTheEventNeverAnAmbientOne() {
        // L'événement porte son tenant ; le déclencheur ne lit jamais le contexte
        // d'exécution, qui n'est plus celui de la requête d'origine.
        UUID otherTenant = UUID.randomUUID();
        when(pfmea.activeProjectOf(otherTenant, PRODUCT)).thenReturn(Optional.of(PROJECT));

        List<RevisionRequest> proposals = trigger.propose(new NcCreatedEvent(
                otherTenant, NC, PRODUCT, null, "Bavure sur alésage", "au poste 20", NOW));

        assertThat(proposals).singleElement()
                .extracting(RevisionRequest::getTenantId).isEqualTo(otherTenant);
        verify(pfmea, never()).activeProjectOf(TENANT, PRODUCT);
    }

    @Test
    void aQuoteInTheTitleDoesNotBreakTheDraftJson() {
        when(pfmea.activeProjectOf(TENANT, PRODUCT)).thenReturn(Optional.of(PROJECT));

        String draft = trigger.propose(new NcCreatedEvent(TENANT, NC, PRODUCT, null,
                        "Bavure \"nette\" sur alésage", "ligne 1\nligne 2", NOW))
                .get(0).getChange().draftJson();

        assertThat(draft).contains("\\\"nette\\\"").doesNotContain("\n");
    }

    @Test
    void aVeryLongTitleIsTruncatedToFitTheLabelColumn() {
        when(pfmea.activeProjectOf(TENANT, PRODUCT)).thenReturn(Optional.of(PROJECT));

        String label = trigger.propose(new NcCreatedEvent(TENANT, NC, PRODUCT, null,
                        "x".repeat(300), null, NOW))
                .get(0).getTriggerRefLabel();

        assertThat(label).hasSize(120);
    }

    @Test
    void aNcWithoutATitleStillCarriesAReadableLabel() {
        when(pfmea.activeProjectOf(TENANT, PRODUCT)).thenReturn(Optional.of(PROJECT));

        String label = trigger.propose(new NcCreatedEvent(TENANT, NC, PRODUCT, null, "  ", null, NOW))
                .get(0).getTriggerRefLabel();

        assertThat(label).contains(NC.toString());
    }

    private void givenItemRated(int occurrence, int detection) {
        when(pfmea.item(TENANT, ITEM)).thenReturn(Optional.of(new PfmeaPort.PfmeaItemSnapshot(
                ITEM, PROJECT, PRODUCT, "Bavure sur alésage", 7, occurrence, detection)));
    }

    private NcCreatedEvent event(UUID fmeaItemId) {
        return new NcCreatedEvent(TENANT, NC, PRODUCT, fmeaItemId,
                "Bavure sur alésage", "constatée au poste 20", NOW);
    }
}
