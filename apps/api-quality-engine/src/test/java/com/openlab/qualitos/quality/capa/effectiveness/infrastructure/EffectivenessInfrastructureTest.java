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
import com.openlab.qualitos.quality.capa.effectiveness.application.ClosedCapaPort;
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
        CapaCase c = cases.save(dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-1"));
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
        CapaCase c = cases.save(dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-1"));
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
        CapaCase un = cases.save(dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-1"));
        CapaCase deux = cases.save(dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-2"));
        CapaCase jamaisJuge = cases.save(dossierSurLePlan(tenant, CapaStatus.OPEN, "CAPA-3"));
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
        CapaCase c = cases.save(dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-1"));
        EffectivenessReviewAdapter port = new EffectivenessReviewAdapter(reviewsJpa);
        port.save(evaluation(tenant, c.getId(), EffectivenessVerdict.PARTIAL, "3 ‰", T0));
        port.save(evaluation(tenant, c.getId(), EffectivenessVerdict.EFFECTIVE, "1 ‰", T0));

        assertThat(port.findLastPerCase(tenant)).hasSize(1);
    }

    @Test
    void leNombreDEvaluationsSeCompteParDossierEtParClient() {
        UUID tenant = UUID.randomUUID();
        UUID autre = UUID.randomUUID();
        CapaCase un = cases.save(dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-1"));
        CapaCase deux = cases.save(dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-2"));
        CapaCase ailleurs = cases.save(dossierSurLePlan(autre, CapaStatus.CLOSED, "CAPA-X"));
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
        CapaCase ailleurs = cases.save(dossierSurLePlan(autre, CapaStatus.CLOSED, "CAPA-X"));
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
        cases.save(dossierSurLePlan(tenant, CapaStatus.OPEN, "CAPA-1"));

        assertThat(port().findInScope(tenant, Optional.empty()).get(0).dueAt()).isNull();
    }

    /**
     * LE PLAN NE JUGE QUE CE QU'UNE ACTION PROMET (ADR 0073, 0088, 0131).
     *
     * <p>Il juge l'EFFET d'une action : sans la décision de vérifier, il n'y a
     * rien à juger, et la ligne ne faisait qu'allonger une liste de travaux à
     * ne pas faire. {@code null} n'est pas un oui : c'est la question laissée
     * ouverte, et elle ne vaut pas décision.
     *
     * <p>Le prédicat porte sur l'ACTION, parce que c'est elle que
     * {@code verifyAction} interroge et qu'elle peut s'écarter de la décision
     * prise à l'ouverture.
     */
    @Test
    void seulesLesActionsAVerifierFontEntrerLeurDossier() {
        UUID tenant = UUID.randomUUID();
        CapaCase corrective = dossier(tenant, CapaStatus.RESOLVED, "CAPA-CORR");
        action(corrective, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);

        CapaCase preventive = dossier(tenant, CapaStatus.RESOLVED, "CAPA-PREV");
        action(preventive, CapaActionStatus.DONE, CapaActionType.PREVENTIVE);
        preventive.getActions().get(0).setVerificationRequired(false);

        // LE CAS « NULL » A CHANGÉ DE CAMP, et il a son propre banc désormais :
        // voir unOuiDuDossierFaitEntrerSesActionsNonTranchees. Il était ici,
        // attendu DEHORS, et c'est ce qui faisait disparaître du plan tout
        // dossier dont le « oui » n'est écrit que sur la fiche -- l'état que
        // laissent `updateCase` et toute ligne antérieure a V177. L'ADR 0131
        // dit que `NULL` veut dire « question non tranchée », pas « non » : le
        // oui du dossier gouverne alors, et ce banc ne garde que ce qui lui
        // appartient en propre -- une réponse POSÉE sur l'action fait foi.

        cases.saveAll(List.of(corrective, preventive));

        assertThat(port().findInScope(tenant, Optional.empty()))
                .extracting(EvaluationCasePort.EvaluationCase::reference)
                .containsExactly("CAPA-CORR");
    }

    /**
     * LE OUI DU DOSSIER GOUVERNE LES ACTIONS QUI N'ONT RIEN TRANCHE.
     *
     * <p>C'est le defaut rapporte depuis la production : on repond « oui » a la
     * verification d'efficacite, et le dossier n'apparait pas au plan. La
     * decision de verifier vit a DEUX niveaux -- sur le dossier (ADR 0073/0108)
     * et sur l'action (ADR 0131) -- et deux chemins d'ecriture courants ne
     * renseignent QUE celui du dossier : la fiche CAPA ({@code updateCase}, qui
     * n'appelle {@code appliquerVerification} que sur le dossier et ne touche
     * aucune action) et toute ligne anterieure a la migration V177, qui a cree
     * les colonnes de l'action SANS reprise.
     *
     * <p>Pour ces dossiers, {@code NULL} sur l'action veut dire « la question
     * n'a pas ete tranchee pour cette action » -- l'ADR 0131 l'ecrit noir sur
     * blanc -- et non « non ». Ne regarder que l'action faisait donc
     * disparaitre du plan un dossier dont la fiche dit « oui », tout en le
     * laissant au registre : exactement le symptome rapporte.
     */
    @Test
    void unOuiDuDossierFaitEntrerSesActionsNonTranchees() {
        UUID tenant = UUID.randomUUID();
        CapaCase parLaFiche = dossier(tenant, CapaStatus.RESOLVED, "CAPA-FICHE");
        action(parLaFiche, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        // Ce que laisse `updateCase`, et ce que V177 a laisse partout : le
        // dossier dit oui, l'action n'a rien tranche.
        parLaFiche.getActions().get(0).setVerificationRequired(null);

        cases.save(parLaFiche);

        assertThat(port().findInScope(tenant, Optional.empty()))
                .extracting(EvaluationCasePort.EvaluationCase::reference)
                .containsExactly("CAPA-FICHE");
    }

    /**
     * UN « NON » EXPLICITE SUR L'ACTION PRIME sur le oui du dossier.
     *
     * <p>Le repli ne vaut que pour une question NON TRANCHEE. Des qu'une action
     * porte une reponse -- meme « non » -- c'est elle qui vaut : quelqu'un a
     * juge, pour cette action, qu'il n'y avait rien a mesurer. Faire remonter
     * le dossier malgre ce « non » rendrait la reponse de l'action sans effet,
     * et on ne saurait plus a quoi sert de la poser.
     *
     * <p>Et le repli ne reanime pas un dossier qui ne demande AUCUNE
     * verification : sans oui nulle part, il n'y a rien a juger.
     */
    @Test
    void unNonSurLActionPrimeSurLeOuiDuDossier() {
        UUID tenant = UUID.randomUUID();
        CapaCase refuse = dossier(tenant, CapaStatus.RESOLVED, "CAPA-NON");
        action(refuse, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        refuse.getActions().get(0).setVerificationRequired(false);

        CapaCase sansDemande = dossier(tenant, CapaStatus.RESOLVED, "CAPA-SANS");
        action(sansDemande, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        sansDemande.getActions().get(0).setVerificationRequired(null);
        sansDemande.setVerificationRequired(null);

        cases.saveAll(List.of(refuse, sansDemande));

        assertThat(port().findInScope(tenant, Optional.empty())).isEmpty();
    }

    /**
     * L'ENDIGUEMENT SEUL N'ENTRE PAS, et on n'a pas eu à l'écrire : le domaine
     * refuse toute vérification sur une action de confinement -- elle arrête
     * l'effet sans supprimer la cause (ISO 9001 §10.2) et ne promet donc rien à
     * mesurer. Aucune de ses actions ne porte le drapeau, le dossier reste
     * dehors. Un dossier MÊLÉ, lui, entre par sa corrective.
     *
     * <p>Un dossier SANS AUCUNE action en reste dehors aussi : il n'y a rien à
     * juger. Son obstacle à lui est le décompte des actions, que le registre
     * dit déjà -- et le faire figurer ici l'aurait montré dans une file où
     * aucun geste ne lui est possible.
     */
    @Test
    void niLEndiguementSeulNiLeDossierSansActionNEntrentDansLePlan() {
        UUID tenant = UUID.randomUUID();
        CapaCase endiguement = dossier(tenant, CapaStatus.RESOLVED, "CAPA-END");
        action(endiguement, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);

        CapaCase mixte = dossier(tenant, CapaStatus.RESOLVED, "CAPA-MIX");
        action(mixte, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        action(mixte, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);

        cases.saveAll(List.of(endiguement, mixte,
                dossier(tenant, CapaStatus.OPEN, "CAPA-SANS-ACTION")));

        List<EvaluationCasePort.EvaluationCase> plan = port().findInScope(tenant, Optional.empty());

        assertThat(plan).extracting(EvaluationCasePort.EvaluationCase::reference)
                .containsExactly("CAPA-MIX");
        // Le drapeau garde son sens pour celui qui entre : son endiguement ne
        // le résume pas.
        assertThat(plan.get(0).containmentOnly()).isFalse();
    }

    @Test
    void unDossierRejeteEstHorsPerimetre() {
        // Un dossier REJETÉ n'a jamais été instruit : il n'y a rien dont juger
        // l'efficacité, et l'afficher gonflerait le décompte des sujets ouverts
        // d'autant de dossiers qu'on a eu raison d'écarter.
        UUID tenant = UUID.randomUUID();
        cases.save(dossierSurLePlan(tenant, CapaStatus.REJECTED, "CAPA-REJ"));
        cases.save(dossierSurLePlan(tenant, CapaStatus.OPEN, "CAPA-OK"));

        assertThat(port().findInScope(tenant, Optional.empty()))
                .extracting(EvaluationCasePort.EvaluationCase::reference)
                .containsExactly("CAPA-OK");
    }


    @Test
    void lesEcartsEncoreOuvertsSeComptentParDossierEtSansLesRegles() {
        UUID tenant = UUID.randomUUID();
        CapaCase avecEcarts = cases.save(dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-NC"));
        CapaCase sansEcart = cases.save(dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-OK"));
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
        cases.save(dossierSurLePlan(tenant, CapaStatus.OPEN, "CAPA-MOI"));
        CapaCase ailleurs = cases.save(dossierSurLePlan(autre, CapaStatus.OPEN, "CAPA-AUTRE"));
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

        CapaCase pilote = dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-PILOTE");
        pilote.setOwnerId(moi);
        cases.save(pilote);

        CapaCase verificateur = dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-VERIF");
        verificateur.setVerificationAssigneeId(moi);
        cases.save(verificateur);

        CapaCase actionConfiee = dossier(tenant, CapaStatus.OPEN, "CAPA-ACTION");
        action(actionConfiee, CapaActionStatus.PENDING, CapaActionType.CORRECTIVE);
        actionConfiee.getActions().get(0).setAssigneeId(moi);
        cases.save(actionConfiee);

        cases.save(dossierSurLePlan(tenant, CapaStatus.OPEN, "CAPA-AUTRUI"));

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
        CapaCase mien = dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-MIEN");
        mien.setOwnerId(moi);
        cases.save(mien);
        CapaCase autrui = cases.save(dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-AUTRUI"));

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

    // ------------------------------------------------- les dossiers clos (taux)

    /**
     * Un dossier dont on a decide, a l'ouverture, qu'aucune verification
     * n'etait exigee n'a rien a faire dans le registre d'efficacite : il est
     * clos, point. Il y figurait pourtant, en attente d'une evaluation que
     * personne ne prononcerait jamais -- ce qui gonflait le retard apparent de
     * dossiers deliberement sortis du perimetre, et noyait ceux qui attendent
     * vraiment.
     */
    @Test
    void unDossierClosSansVerificationExigeeNEstPasDansLeRegistre() {
        UUID tenant = UUID.randomUUID();
        CapaCase exige = dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-EXI");
        exige.setVerificationRequired(true);
        cases.save(exige);
        CapaCase dispense = dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-DIS");
        dispense.setVerificationRequired(false);
        cases.save(dispense);

        assertThat(closedPort().findClosed(tenant))
                .extracting(ClosedCapaPort.ClosedCapa::title)
                .containsExactly("Dossier CAPA-EXI");
    }

    /**
     * Seul {@code FALSE} exclut. {@code null} veut dire « pas encore tranche »,
     * et ne pas avoir repondu n'est pas avoir repondu non : ces dossiers-la
     * restent visibles, parce que c'est precisement le registre qui fait
     * apparaitre la question oubliee.
     */
    @Test
    void unDossierClosDontLaQuestionNaPasEteTrancheeResteVisible() {
        UUID tenant = UUID.randomUUID();
        CapaCase indecis = dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-NUL");
        indecis.setVerificationRequired(null);
        cases.save(indecis);

        assertThat(closedPort().findClosed(tenant))
                .extracting(ClosedCapaPort.ClosedCapa::title)
                .containsExactly("Dossier CAPA-NUL");
    }

    /** Un dossier qui n'est pas CLOS n'est pas un dossier clos. */
    @Test
    void seulsLesDossiersClosEntrentDansLeRegistre() {
        UUID tenant = UUID.randomUUID();
        cases.save(dossierSurLePlan(tenant, CapaStatus.CLOSED, "CAPA-CLOS"));
        cases.save(dossierSurLePlan(tenant, CapaStatus.RESOLVED, "CAPA-RES"));
        cases.save(dossierSurLePlan(tenant, CapaStatus.REJECTED, "CAPA-REJ"));

        assertThat(closedPort().findClosed(tenant))
                .extracting(ClosedCapaPort.ClosedCapa::title)
                .containsExactly("Dossier CAPA-CLOS");
    }

    private ClosedCapaAdapter closedPort() {
        return new ClosedCapaAdapter(cases, nonConformities);
    }

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
        c.setVerificationRequired(true);
        return c;
    }

    /**
     * Un dossier DÉJÀ SUR LE PLAN : il porte l'action corrective à vérifier qui
     * l'y fait entrer. Les bancs qui portent sur autre chose -- le périmètre,
     * les décomptes, l'échéance -- passent par ici, pour ne pas redire à chaque
     * fois ce qu'ils ne mesurent pas.
     */
    private static CapaCase dossierSurLePlan(UUID tenant, CapaStatus statut, String ref) {
        CapaCase c = dossier(tenant, statut, ref);
        action(c, CapaActionStatus.PENDING, CapaActionType.CORRECTIVE);
        return c;
    }

    private static void action(CapaCase c, CapaActionStatus statut, CapaActionType type) {
        CapaAction a = new CapaAction();
        a.setCapa(c);
        a.setTitle("Action " + statut + " " + type);
        a.setStatus(statut);
        a.setActionType(type);
        // Comme `openCase` : la décision de vérification descend sur l'action,
        // sauf sous un endiguement -- le domaine la lui refuse, il ne promet
        // aucun effet à mesurer. C'est ce drapeau qui fait entrer le dossier
        // dans le plan de travail.
        if (type != CapaActionType.CONTAINMENT) {
            a.setVerificationRequired(true);
        }
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
