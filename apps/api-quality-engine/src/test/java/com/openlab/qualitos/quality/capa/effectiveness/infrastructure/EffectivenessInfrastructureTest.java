package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.CapaAction;
import com.openlab.qualitos.quality.capa.CapaActionStatus;
import com.openlab.qualitos.quality.capa.CapaActionType;
import com.openlab.qualitos.quality.capa.CapaCase;
import com.openlab.qualitos.quality.capa.CapaCaseRepository;
import com.openlab.qualitos.quality.capa.CapaCriticity;
import com.openlab.qualitos.quality.capa.CapaSourceType;
import com.openlab.qualitos.quality.capa.CapaStatus;
import com.openlab.qualitos.quality.capa.CapaType;
import com.openlab.qualitos.quality.capa.effectiveness.application.EvaluationCasePort;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les deux adaptateurs de l'évaluation d'efficacité, sur le mapping JPA réel
 * (contexte complet, H2 en mode PostgreSQL) — même idiome que
 * {@code SmiQueriesIntegrationTest}.
 *
 * <p>Un dépôt simulé rendrait ce qu'on lui donne et ne dirait rien des deux
 * choses qui cassent ici pour de vrai : la requête « la dernière évaluation de
 * chaque dossier », écrite en JPQL portable (H2 ici, PostgreSQL en production)
 * parce qu'une fenêtre {@code row_number()} ne s'exécute pas partout, et le
 * cloisonnement par client, qui ne se voit jamais tant qu'un seul client existe
 * dans le jeu de données.
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("web")
class EffectivenessInfrastructureTest {

    static final Instant T0 = Instant.parse("2026-11-01T08:00:00Z").truncatedTo(ChronoUnit.MILLIS);

    /** Les références de NC sont uniques en base : un compteur évite les collisions. */
    private static final AtomicInteger NC_SEQ = new AtomicInteger();

    @Autowired EffectivenessReviewJpaRepository reviewsJpa;
    @Autowired CapaCaseRepository cases;
    @Autowired NonConformityRepository nonConformities;

    // ---------------------------------------------------------------- reviews

    @Test
    void uneEvaluationSEnregistreEtSeRelitAChampsEgaux() {
        UUID tenant = UUID.randomUUID();
        CapaCase c = cases.save(dossier(tenant, CapaStatus.CLOSED, "CAPA-1"));
        EffectivenessReviewAdapter port = new EffectivenessReviewAdapter(reviewsJpa);

        port.save(EffectivenessReview.record(UUID.randomUUID(), tenant, c.getId(),
                EffectivenessVerdict.PARTIAL, "Moins de 2 ‰ sur 3 mois",
                "7,4 ‰", "< 2 ‰", "3,1 ‰", "Mesuré sur les lots 2411 à 2418",
                UUID.randomUUID(), "A. Diallo", T0));

        EffectivenessReview relue = port.findLastByCase(tenant, c.getId()).orElseThrow();
        assertThat(relue.verdict()).isEqualTo(EffectivenessVerdict.PARTIAL);
        assertThat(relue.criterion()).isEqualTo("Moins de 2 ‰ sur 3 mois");
        assertThat(relue.baseline()).isEqualTo("7,4 ‰");
        assertThat(relue.target()).isEqualTo("< 2 ‰");
        assertThat(relue.observed()).isEqualTo("3,1 ‰");
        assertThat(relue.comment()).isEqualTo("Mesuré sur les lots 2411 à 2418");
        assertThat(relue.reviewerName()).isEqualTo("A. Diallo");
        assertThat(relue.reviewedAt()).isEqualTo(T0);
    }

    @Test
    void lHistoriqueSeLitDuPlusRecentAuPlusAncien() {
        // C'est la séquence que lit un auditeur : « inefficace, puis partiel,
        // puis efficace » raconte une boucle d'amélioration ; le seul dernier
        // verdict ne raconte rien.
        UUID tenant = UUID.randomUUID();
        CapaCase c = cases.save(dossier(tenant, CapaStatus.CLOSED, "CAPA-1"));
        EffectivenessReviewAdapter port = new EffectivenessReviewAdapter(reviewsJpa);

        port.save(evaluation(tenant, c.getId(), EffectivenessVerdict.INEFFECTIVE, "9 ‰", T0));
        port.save(evaluation(tenant, c.getId(), EffectivenessVerdict.PARTIAL, "3 ‰",
                T0.plusSeconds(86_400)));
        port.save(evaluation(tenant, c.getId(), EffectivenessVerdict.EFFECTIVE, "1 ‰",
                T0.plusSeconds(172_800)));

        assertThat(port.findByCase(tenant, c.getId()))
                .extracting(EffectivenessReview::verdict)
                .containsExactly(EffectivenessVerdict.EFFECTIVE, EffectivenessVerdict.PARTIAL,
                        EffectivenessVerdict.INEFFECTIVE);
        assertThat(port.findLastByCase(tenant, c.getId()).orElseThrow().observed()).isEqualTo("1 ‰");
    }

    @Test
    void laDerniereEvaluationDeChaqueDossierTientEnUneRequete() {
        UUID tenant = UUID.randomUUID();
        CapaCase un = cases.save(dossier(tenant, CapaStatus.CLOSED, "CAPA-1"));
        CapaCase deux = cases.save(dossier(tenant, CapaStatus.RESOLVED, "CAPA-2"));
        CapaCase jamaisJuge = cases.save(dossier(tenant, CapaStatus.OPEN, "CAPA-3"));
        EffectivenessReviewAdapter port = new EffectivenessReviewAdapter(reviewsJpa);
        port.save(evaluation(tenant, un.getId(), EffectivenessVerdict.INEFFECTIVE, "9 ‰", T0));
        port.save(evaluation(tenant, un.getId(), EffectivenessVerdict.EFFECTIVE, "1 ‰",
                T0.plusSeconds(86_400)));
        port.save(evaluation(tenant, deux.getId(), EffectivenessVerdict.PARTIAL, "3 ‰", T0));

        Map<UUID, EffectivenessReview> last = port.findLastPerCase(tenant);

        assertThat(last).hasSize(2);
        assertThat(last.get(un.getId()).verdict()).isEqualTo(EffectivenessVerdict.EFFECTIVE);
        assertThat(last.get(deux.getId()).verdict()).isEqualTo(EffectivenessVerdict.PARTIAL);
        assertThat(last).doesNotContainKey(jamaisJuge.getId());
    }

    @Test
    void deuxEvaluationsAuMemeInstantNeRendentQuUneSeuleDerniere() {
        // Deux enregistrements à la même milliseconde sont rares et arrivent :
        // sans départage sur l'identifiant, la requête « la dernière » en
        // rendrait DEUX pour un seul dossier, et la carte en garderait une au
        // hasard — un tableau qui change d'avis d'un rafraîchissement à l'autre.
        UUID tenant = UUID.randomUUID();
        CapaCase c = cases.save(dossier(tenant, CapaStatus.CLOSED, "CAPA-1"));
        EffectivenessReviewAdapter port = new EffectivenessReviewAdapter(reviewsJpa);
        port.save(evaluation(tenant, c.getId(), EffectivenessVerdict.PARTIAL, "3 ‰", T0));
        port.save(evaluation(tenant, c.getId(), EffectivenessVerdict.EFFECTIVE, "1 ‰", T0));

        assertThat(port.findLastPerCase(tenant)).hasSize(1);
    }

    @Test
    void leNombreDEvaluationsSeCompteParDossierEtParClient() {
        UUID tenant = UUID.randomUUID();
        UUID autre = UUID.randomUUID();
        CapaCase un = cases.save(dossier(tenant, CapaStatus.CLOSED, "CAPA-1"));
        CapaCase deux = cases.save(dossier(tenant, CapaStatus.CLOSED, "CAPA-2"));
        CapaCase ailleurs = cases.save(dossier(autre, CapaStatus.CLOSED, "CAPA-X"));
        EffectivenessReviewAdapter port = new EffectivenessReviewAdapter(reviewsJpa);
        port.save(evaluation(tenant, un.getId(), EffectivenessVerdict.INEFFECTIVE, "9 ‰", T0));
        port.save(evaluation(tenant, un.getId(), EffectivenessVerdict.EFFECTIVE, "1 ‰",
                T0.plusSeconds(60)));
        port.save(evaluation(tenant, deux.getId(), EffectivenessVerdict.PARTIAL, "3 ‰", T0));
        port.save(evaluation(autre, ailleurs.getId(), EffectivenessVerdict.EFFECTIVE, "1 ‰", T0));

        Map<UUID, Integer> counts = port.countPerCase(tenant);

        assertThat(counts).containsEntry(un.getId(), 2).containsEntry(deux.getId(), 1);
        assertThat(counts).doesNotContainKey(ailleurs.getId());
    }

    @Test
    void lesEvaluationsDUnAutreClientSontInvisibles() {
        UUID tenant = UUID.randomUUID();
        UUID autre = UUID.randomUUID();
        CapaCase ailleurs = cases.save(dossier(autre, CapaStatus.CLOSED, "CAPA-X"));
        EffectivenessReviewAdapter port = new EffectivenessReviewAdapter(reviewsJpa);
        port.save(evaluation(autre, ailleurs.getId(), EffectivenessVerdict.EFFECTIVE, "1 ‰", T0));

        assertThat(port.findByCase(tenant, ailleurs.getId())).isEmpty();
        assertThat(port.findLastByCase(tenant, ailleurs.getId())).isEmpty();
        assertThat(port.findLastPerCase(tenant)).doesNotContainKey(ailleurs.getId());
    }

    // ------------------------------------------------------- evaluation cases

    @Test
    void unDossierArriveAvecSesDecomptesEtSonEcheanceAuSoir() {
        UUID tenant = UUID.randomUUID();
        CapaCase c = dossier(tenant, CapaStatus.RESOLVED, "CAPA-1");
        c.setEffectivenessCriterion("Moins de 2 ‰ sur 3 mois");
        c.setOwnerName("A. Diallo");
        c.setDueDate(LocalDate.of(2026, 12, 1));
        action(c, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        action(c, CapaActionStatus.PENDING, CapaActionType.PREVENTIVE);
        cases.save(c);

        EvaluationCasePort.EvaluationCase lu = port().findInScope(tenant, Optional.empty()).get(0);

        assertThat(lu.reference()).isEqualTo("CAPA-1");
        assertThat(lu.title()).isEqualTo("Dossier CAPA-1");
        assertThat(lu.criticity()).isEqualTo("HIGH");
        assertThat(lu.ownerName()).isEqualTo("A. Diallo");
        assertThat(lu.criterion()).isEqualTo("Moins de 2 ‰ sur 3 mois");
        assertThat(lu.closed()).isFalse();
        assertThat(lu.resolved()).isTrue();
        assertThat(lu.actionsTotal()).isEqualTo(2);
        assertThat(lu.actionsDone()).isEqualTo(1);
        assertThat(lu.containmentOnly()).isFalse();
        assertThat(lu.openNcCount()).isZero();
        // Une échéance est tenue jusqu'au SOIR : la ramener à minuit afficherait
        // en retard, dès son premier instant, un dossier dû le jour même.
        assertThat(lu.dueAt())
                .isEqualTo(LocalDate.of(2026, 12, 1).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC));
    }

    @Test
    void unDossierSansEcheanceNArrivePasAvecUneDateInventee() {
        UUID tenant = UUID.randomUUID();
        cases.save(dossier(tenant, CapaStatus.OPEN, "CAPA-1"));

        assertThat(port().findInScope(tenant, Optional.empty()).get(0).dueAt()).isNull();
    }

    @Test
    void unDossierRejeteEstHorsPerimetre() {
        // Un dossier REJETÉ n'a jamais été instruit : il n'y a rien dont juger
        // l'efficacité, et l'afficher gonflerait le décompte des sujets ouverts
        // d'autant de dossiers qu'on a eu raison d'écarter.
        UUID tenant = UUID.randomUUID();
        cases.save(dossier(tenant, CapaStatus.REJECTED, "CAPA-REJ"));
        cases.save(dossier(tenant, CapaStatus.OPEN, "CAPA-OK"));

        assertThat(port().findInScope(tenant, Optional.empty()))
                .extracting(EvaluationCasePort.EvaluationCase::reference)
                .containsExactly("CAPA-OK");
    }

    @Test
    void lEndiguementSeulSeVoitEtUnDossierVideNEnEstPasUn() {
        UUID tenant = UUID.randomUUID();
        CapaCase endiguement = dossier(tenant, CapaStatus.RESOLVED, "CAPA-END");
        action(endiguement, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        action(endiguement, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        CapaCase mixte = dossier(tenant, CapaStatus.RESOLVED, "CAPA-MIX");
        action(mixte, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        action(mixte, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        CapaCase vide = dossier(tenant, CapaStatus.OPEN, "CAPA-VIDE");
        cases.saveAll(List.of(endiguement, mixte, vide));

        Map<String, EvaluationCasePort.EvaluationCase> parRef = parReference(tenant);

        assertThat(parRef.get("CAPA-END").containmentOnly()).isTrue();
        assertThat(parRef.get("CAPA-MIX").containmentOnly()).isFalse();
        // Un allMatch sur une liste vide répondrait « oui » à une question qui
        // ne se pose pas : le dossier sans action a son propre motif.
        assertThat(parRef.get("CAPA-VIDE").containmentOnly()).isFalse();
        assertThat(parRef.get("CAPA-VIDE").actionsTotal()).isZero();
    }

    @Test
    void lesEcartsEncoreOuvertsSeComptentParDossierEtSansLesRegles() {
        UUID tenant = UUID.randomUUID();
        CapaCase avecEcarts = cases.save(dossier(tenant, CapaStatus.RESOLVED, "CAPA-NC"));
        CapaCase sansEcart = cases.save(dossier(tenant, CapaStatus.RESOLVED, "CAPA-OK"));
        nonConformities.saveAll(List.of(
                ecart(tenant, avecEcarts.getId(), NcStatus.OPEN),
                ecart(tenant, avecEcarts.getId(), NcStatus.UNDER_ANALYSIS),
                // Réglés : un constat clos, annulé ou rejeté n'a pas lieu
                // d'empêcher la clôture du dossier qui l'a traité.
                ecart(tenant, avecEcarts.getId(), NcStatus.CLOSED),
                ecart(tenant, avecEcarts.getId(), NcStatus.CANCELLED),
                ecart(tenant, avecEcarts.getId(), NcStatus.REJECTED)));

        Map<String, EvaluationCasePort.EvaluationCase> parRef = parReference(tenant);

        assertThat(parRef.get("CAPA-NC").openNcCount()).isEqualTo(2);
        assertThat(parRef.get("CAPA-OK").openNcCount()).isZero();
        assertThat(sansEcart.getId()).isNotEqualTo(avecEcarts.getId());
    }

    @Test
    void unDossierDUnAutreClientNEntrePasDansLePerimetre() {
        UUID tenant = UUID.randomUUID();
        UUID autre = UUID.randomUUID();
        cases.save(dossier(tenant, CapaStatus.OPEN, "CAPA-MOI"));
        CapaCase ailleurs = cases.save(dossier(autre, CapaStatus.OPEN, "CAPA-AUTRE"));
        nonConformities.save(ecart(autre, ailleurs.getId(), NcStatus.OPEN));

        assertThat(port().findInScope(tenant, Optional.empty()))
                .extracting(EvaluationCasePort.EvaluationCase::reference)
                .containsExactly("CAPA-MOI");
    }

    @Test
    void unDossierClosArriveAvecSonDrapeauDeCloture() {
        UUID tenant = UUID.randomUUID();
        CapaCase c = dossier(tenant, CapaStatus.CLOSED, "CAPA-CLOS");
        action(c, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        cases.save(c);

        EvaluationCasePort.EvaluationCase lu = port().findInScope(tenant, Optional.empty()).get(0);

        assertThat(lu.closed()).isTrue();
        assertThat(lu.resolved()).isFalse();
    }

    // ------------------------------------- la portée par utilisateur (ADR 0081)

    @Test
    void sansVoirToutOnNeVoitQueLesDossiersQuiNousConcernent() {
        // Les trois façons dont un dossier concerne quelqu'un : il le pilote, il
        // en vérifie l'efficacité, ou une de ses actions lui est confiée. Une
        // seule de ces trois branches oubliée dans la requête, et une personne
        // perd de vue un dossier dont elle est responsable — le défaut inverse
        // de la fuite, et tout aussi réel.
        UUID tenant = UUID.randomUUID();
        UUID moi = UUID.randomUUID();

        CapaCase pilote = dossier(tenant, CapaStatus.RESOLVED, "CAPA-PILOTE");
        pilote.setOwnerId(moi);
        cases.save(pilote);

        CapaCase verificateur = dossier(tenant, CapaStatus.RESOLVED, "CAPA-VERIF");
        verificateur.setVerificationAssigneeId(moi);
        cases.save(verificateur);

        CapaCase actionConfiee = dossier(tenant, CapaStatus.OPEN, "CAPA-ACTION");
        action(actionConfiee, CapaActionStatus.PENDING, CapaActionType.CORRECTIVE);
        actionConfiee.getActions().get(0).setAssigneeId(moi);
        cases.save(actionConfiee);

        cases.save(dossier(tenant, CapaStatus.OPEN, "CAPA-AUTRUI"));

        assertThat(port().findInScope(tenant, Optional.of(moi)))
                .extracting(EvaluationCasePort.EvaluationCase::reference)
                .containsExactlyInAnyOrder("CAPA-PILOTE", "CAPA-VERIF", "CAPA-ACTION");
    }

    @Test
    void unDossierRestreintArriveAvecTOUTESSesActions() {
        // Le piège de la requête restreinte : filtrer sur la jointure ramenée
        // au lieu d'un `exists` livrerait le dossier avec les seules actions de
        // cet utilisateur, et son décompte « 1/1 faite » serait faux — il
        // s'ouvrirait au jugement alors qu'une action reste à mener.
        UUID tenant = UUID.randomUUID();
        UUID moi = UUID.randomUUID();
        CapaCase c = dossier(tenant, CapaStatus.RESOLVED, "CAPA-MIXTE");
        action(c, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        action(c, CapaActionStatus.PENDING, CapaActionType.PREVENTIVE);
        c.getActions().get(0).setAssigneeId(moi);
        cases.save(c);

        EvaluationCasePort.EvaluationCase lu =
                port().findInScope(tenant, Optional.of(moi)).get(0);

        assertThat(lu.actionsTotal()).isEqualTo(2);
        assertThat(lu.actionsDone()).isEqualTo(1);
    }

    @Test
    void laRestrictionNeFranchitPasLaFrontiereDuClient() {
        UUID tenant = UUID.randomUUID();
        UUID autreClient = UUID.randomUUID();
        UUID moi = UUID.randomUUID();
        CapaCase ailleurs = dossier(autreClient, CapaStatus.RESOLVED, "CAPA-AILLEURS");
        ailleurs.setOwnerId(moi);
        cases.save(ailleurs);

        assertThat(port().findInScope(tenant, Optional.of(moi))).isEmpty();
    }

    @Test
    void unDossierHorsDePorteeNEstPasVu() {
        UUID tenant = UUID.randomUUID();
        UUID moi = UUID.randomUUID();
        CapaCase mien = dossier(tenant, CapaStatus.RESOLVED, "CAPA-MIEN");
        mien.setOwnerId(moi);
        cases.save(mien);
        CapaCase autrui = cases.save(dossier(tenant, CapaStatus.RESOLVED, "CAPA-AUTRUI"));

        assertThat(port().sees(tenant, mien.getId(), Optional.of(moi))).isTrue();
        assertThat(port().sees(tenant, autrui.getId(), Optional.of(moi))).isFalse();
        // Sans restriction, tout dossier du client est vu.
        assertThat(port().sees(tenant, autrui.getId(), Optional.empty())).isTrue();
    }

    @Test
    void unDossierDUnAutreClientNEstJamaisVu() {
        UUID tenant = UUID.randomUUID();
        CapaCase ailleurs = cases.save(
                dossier(UUID.randomUUID(), CapaStatus.RESOLVED, "CAPA-AILLEURS"));

        assertThat(port().sees(tenant, ailleurs.getId(), Optional.empty())).isFalse();
    }

    @Test
    void unIdentifiantInconnuNEstPasVu() {
        assertThat(port().sees(UUID.randomUUID(), UUID.randomUUID(), Optional.empty()))
                .isFalse();
    }

    // -------------------------------------------------------------- outillage

    private EvaluationCaseAdapter port() {
        return new EvaluationCaseAdapter(cases, nonConformities);
    }

    private Map<String, EvaluationCasePort.EvaluationCase> parReference(UUID tenant) {
        return port().findInScope(tenant, Optional.empty()).stream()
                .collect(java.util.stream.Collectors.toMap(
                        EvaluationCasePort.EvaluationCase::reference, e -> e));
    }

    private static EffectivenessReview evaluation(UUID tenant, UUID capaId,
                                                  EffectivenessVerdict verdict,
                                                  String observed, Instant at) {
        return EffectivenessReview.record(UUID.randomUUID(), tenant, capaId, verdict,
                "Moins de 2 ‰ sur 3 mois", "7,4 ‰", "< 2 ‰", observed, null,
                UUID.randomUUID(), "A. Diallo", at.truncatedTo(ChronoUnit.MILLIS));
    }

    private static CapaCase dossier(UUID tenant, CapaStatus statut, String ref) {
        CapaCase c = new CapaCase();
        c.setTenantId(tenant);
        c.setTitle("Dossier " + ref);
        c.setType(CapaType.CORRECTIVE);
        c.setCriticity(CapaCriticity.HIGH);
        c.setStatus(statut);
        c.setSourceType(CapaSourceType.NON_CONFORMITY);
        c.setSourceRef(ref);
        c.setOwnerId(UUID.randomUUID());
        return c;
    }

    private static void action(CapaCase c, CapaActionStatus statut, CapaActionType type) {
        CapaAction a = new CapaAction();
        a.setCapa(c);
        a.setTitle("Action " + statut + " " + type);
        a.setStatus(statut);
        a.setActionType(type);
        c.getActions().add(a);
    }

    private static NonConformity ecart(UUID tenant, UUID capaId, NcStatus statut) {
        NonConformity nc = new NonConformity();
        nc.setTenantId(tenant);
        nc.setReference("NC-EFF-" + NC_SEQ.incrementAndGet());
        nc.setTitle("Écart " + statut);
        nc.setCategory(NcCategory.PRODUCT);
        nc.setSeverity(NcSeverity.MAJOR);
        nc.setStatus(statut);
        nc.setDetectedAt(T0);
        nc.setCapaCaseId(capaId);
        return nc;
    }
}
