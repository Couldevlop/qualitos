package com.openlab.qualitos.quality.revisionrequests.infrastructure;

import com.openlab.qualitos.quality.revisionrequests.domain.ProposedChange;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequest;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionRequestRepository;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTargetType;
import com.openlab.qualitos.quality.revisionrequests.domain.RevisionTriggerType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce que la non-conformité demande au registre des révisions : « cet écart
 * a-t-il déjà fait réviser le PFMEA, ou le plan de surveillance ? »
 *
 * <p>La réponse commande un verrou de clôture. Elle doit donc distinguer ce qui
 * a été <b>acté</b> de ce qui a seulement été proposé : une proposition en
 * attente montre qu'on a vu la question, pas qu'on l'a tranchée.
 */
class NcRiskRevisionsAdapterTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID NC = UUID.randomUUID();
    private static final UUID PRODUIT = UUID.randomUUID();

    private final RevisionRequestRepository repo = mock(RevisionRequestRepository.class);
    private final NcRiskRevisionsAdapter adapter = new NcRiskRevisionsAdapter(repo);

    private static RevisionRequest demande(RevisionTargetType cible) {
        return RevisionRequest.propose(TENANT, PRODUIT, cible,
                cible.isCreation() ? null : UUID.randomUUID(),
                RevisionTriggerType.NC_CREATED, NC, "Fuite au presse-étoupe",
                "2 NC en 12 mois",
                ProposedChange.rating("occurrence", 3, 5), Instant.now());
    }

    /** Une révision du PFMEA acceptée : la leçon est tirée, le verrou tombe. */
    @Test
    void pfmeaAccepte_repondOui() {
        RevisionRequest r = demande(RevisionTargetType.PFMEA_ITEM);
        r.accept(UUID.randomUUID(), Instant.now());
        when(repo.findByTrigger(TENANT, NC)).thenReturn(List.of(r));

        assertThat(adapter.pfmeaRevisedFor(TENANT, NC)).isTrue();
        assertThat(adapter.controlPlanRevisedFor(TENANT, NC)).isFalse();
    }

    /**
     * UNE PROPOSITION EN ATTENTE NE COMPTE PAS.
     *
     * <p>Le module en crée une automatiquement dès qu'un écart est constaté sur
     * un produit. La prendre pour une révision reviendrait à lever le verrou
     * avant que personne n'ait rien décidé — et le PFMEA resterait muet sur un
     * défaut qui est survenu pour de bon.
     */
    @Test
    void pfmeaSeulementPropose_repondNon() {
        when(repo.findByTrigger(TENANT, NC))
                .thenReturn(List.of(demande(RevisionTargetType.PFMEA_ITEM)));

        assertThat(adapter.pfmeaRevisedFor(TENANT, NC)).isFalse();
    }

    /**
     * UNE PROPOSITION REJETÉE NE COMPTE PAS NON PLUS : elle dit qu'on ne révise
     * pas, ce qui contredit la déclaration d'impact. La sortie est alors de
     * corriger la réponse, pas de forcer la clôture.
     */
    @Test
    void pfmeaRejete_repondNon() {
        RevisionRequest r = demande(RevisionTargetType.PFMEA_ITEM);
        r.reject(UUID.randomUUID(), "Cote déjà au maximum", Instant.now());
        when(repo.findByTrigger(TENANT, NC)).thenReturn(List.of(r));

        assertThat(adapter.pfmeaRevisedFor(TENANT, NC)).isFalse();
    }

    /** La création d'une ligne de PFMEA est une révision du PFMEA, elle aussi. */
    @Test
    void creationDeLignePfmeaAcceptee_repondOui() {
        RevisionRequest r = demande(RevisionTargetType.PFMEA_ITEM_CREATE);
        r.accept(UUID.randomUUID(), Instant.now());
        when(repo.findByTrigger(TENANT, NC)).thenReturn(List.of(r));

        assertThat(adapter.pfmeaRevisedFor(TENANT, NC)).isTrue();
    }

    /** Le plan de surveillance répond à sa propre question, et pas à celle du PFMEA. */
    @Test
    void planDeSurveillanceAccepte_repondOuiPourLuiSeul() {
        RevisionRequest r = demande(RevisionTargetType.CONTROL_PLAN_LINE_CREATE);
        r.accept(UUID.randomUUID(), Instant.now());
        when(repo.findByTrigger(TENANT, NC)).thenReturn(List.of(r));

        assertThat(adapter.controlPlanRevisedFor(TENANT, NC)).isTrue();
        assertThat(adapter.pfmeaRevisedFor(TENANT, NC)).isFalse();
    }

    /** Aucune demande : rien n'a été révisé, et la question n'interroge pas la base. */
    @Test
    void aucuneDemande_repondNon() {
        when(repo.findByTrigger(TENANT, NC)).thenReturn(List.of());

        assertThat(adapter.pfmeaRevisedFor(TENANT, NC)).isFalse();
        assertThat(adapter.controlPlanRevisedFor(TENANT, NC)).isFalse();
    }

    /**
     * Un identifiant manquant répond « non » sans interroger la base : la
     * question n'a pas de sens, et une requête sur {@code null} chercherait un
     * déclencheur que personne ne porte.
     */
    @Test
    void identifiantManquant_repondNonSansRequete() {
        when(repo.findByTrigger(any(), any()))
                .thenThrow(new AssertionError("la base ne doit pas être interrogée"));

        assertThat(adapter.pfmeaRevisedFor(null, NC)).isFalse();
        assertThat(adapter.pfmeaRevisedFor(TENANT, null)).isFalse();
        assertThat(adapter.controlPlanRevisedFor(null, null)).isFalse();
    }

    /** Le port est bien ce que la non-conformité attend : un contrat, pas une classe. */
    @Test
    void estBienLePortAttenduParLaNonConformite() {
        assertThat(Optional.of(adapter))
                .get()
                .isInstanceOf(com.openlab.qualitos.quality.nonconformity.NcRiskRevisionsPort.class);
    }
}
