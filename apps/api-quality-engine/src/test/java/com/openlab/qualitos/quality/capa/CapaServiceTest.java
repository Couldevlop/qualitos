package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.openlab.qualitos.quality.nonconformity.NcOrigin;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapaServiceTest {

    @Mock CapaCaseRepository caseRepo;
    @Mock CapaActionRepository actionRepo;
    @Mock CapaLifecycleJournal journal;
    @Mock com.openlab.qualitos.quality.nonconformity.NonConformityRepository ncRepo;
    @Mock CapaEvidenceRepository evidenceRepo;
    /** Sans réglage, Mockito rend Optional.empty() : l'utilisateur voit tout (ADR 0081). */
    @Mock RecordScope scope;
    /** L'historique des évaluations d'efficacité (ADR 0108). */
    @Mock com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessReviewPort reviews;
    @Mock ObjectiveRecoveryPort objectiveRecovery;
    @InjectMocks CapaService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    // --- create ---
    @Test
    void create_success() {
        CapaDto.CreateCaseRequest req = req();
        CapaCase saved = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.save(any())).thenReturn(saved);

        CapaDto.CaseResponse r = service.createCase(req);

        assertThat(r.status()).isEqualTo(CapaStatus.OPEN);
        assertThat(r.tenantId()).isEqualTo(TENANT);
    }

    @Test
    void create_missingTenant_throws() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.createCase(req()))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(caseRepo);
    }

    // --- findAll ---
    @Test
    void findAll_noFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(caseRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.OPEN))));
        Page<CapaDto.CaseResponse> r = service.findAll(null, p);
        assertThat(r.getContent()).hasSize(1);
        verify(caseRepo, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    @Test
    void findAll_withFilter() {
        Pageable p = PageRequest.of(0, 10);
        when(caseRepo.findByTenantIdAndStatus(TENANT, CapaStatus.CLOSED, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.CLOSED))));
        Page<CapaDto.CaseResponse> r = service.findAll(CapaStatus.CLOSED, p);
        assertThat(r.getContent().get(0).status()).isEqualTo(CapaStatus.CLOSED);
    }

    @Test
    void findAll_byCriticity_filtersInTheDatabase() {
        Pageable p = PageRequest.of(0, 10);
        when(caseRepo.search(TENANT, CapaStatus.OPEN, CapaCriticity.CRITICAL, null, null, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.OPEN))));
        assertThat(service.findAll(CapaStatus.OPEN, CapaCriticity.CRITICAL, p).getContent()).hasSize(1);
        verify(caseRepo, never()).findByTenantIdAndStatus(any(), any(), any());
        verify(caseRepo, never()).findByTenantId(any(), any());
    }

    /**
     * L'ECHEANCE LIMITE FILTRE EN BASE, comme le pilote et la criticite : la
     * retenir sur la page rendue n'aurait garde que les dossiers a echoir
     * PRESENTS SUR CETTE PAGE, et le compteur aurait annonce le total de tous
     * les autres.
     */
    @Test
    void findAll_byDueBefore_filtersInTheDatabase() {
        Pageable p = PageRequest.of(0, 10);
        LocalDate limite = LocalDate.parse("2026-12-15");
        when(caseRepo.search(TENANT, null, null, null, limite, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.OPEN))));

        assertThat(service.findAll(null, null, null, limite, p).getContent()).hasSize(1);

        verify(caseRepo, never()).findByTenantId(any(), any());
        verify(caseRepo, never()).findByTenantIdAndStatus(any(), any(), any());
    }

    // --- findById ---
    @Test
    void findById_found() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThat(service.findById(c.getId()).id()).isEqualTo(c.getId());
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(caseRepo.findByIdAndTenantId(id, TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(CapaNotFoundException.class);
    }

    @Test
    void findById_wrongTenant_notFound() {
        CapaCase c = capa(OTHER, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(c.getId()))
                .isInstanceOf(CapaNotFoundException.class);
    }

    // --- update ---
    @Test
    void update_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        UUID rc = UUID.randomUUID();
        LocalDate due = LocalDate.now().plusDays(10);
        CapaDto.UpdateCaseRequest req = new CapaDto.UpdateCaseRequest(
                "t2", "d2", CapaCriticity.CRITICAL, "ref2", rc, due,
                null, null, null, null, null, null);
        service.updateCase(c.getId(), req);
        assertThat(c.getTitle()).isEqualTo("t2");
        assertThat(c.getCriticity()).isEqualTo(CapaCriticity.CRITICAL);
        assertThat(c.getRootCauseId()).isEqualTo(rc);
        assertThat(c.getDueDate()).isEqualTo(due);
    }

    @Test
    void update_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateCase(c.getId(),
                new CapaDto.UpdateCaseRequest("x", null, null, null, null, null,
                        null, null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void update_rejected_throws() {
        CapaCase c = capa(TENANT, CapaStatus.REJECTED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateCase(c.getId(),
                new CapaDto.UpdateCaseRequest("x", null, null, null, null, null,
                        null, null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- verification exigee ---

    /** Raccourci de lisibilite : seuls les quatre champs de verification varient. */
    private CapaDto.UpdateCaseRequest verification(Boolean exigee, UUID qui,
                                                   String nom, String consignes) {
        return new CapaDto.UpdateCaseRequest(null, null, null, null, null, null,
                exigee, qui, nom, consignes, null, null);
    }

    @Test
    void verification_exigee_et_assignee_est_enregistree() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        UUID verificateur = UUID.randomUUID();

        service.updateCase(c.getId(), verification(
                true, verificateur, "  controle@exemple.fr  ",
                "  Reprendre 30 pieces au calibre apres 2 semaines de production.  "));

        assertThat(c.getVerificationRequired()).isTrue();
        assertThat(c.getVerificationAssigneeId()).isEqualTo(verificateur);
        // Rogne : un nom entoure d'espaces est le meme nom.
        assertThat(c.getVerificationAssigneeName()).isEqualTo("controle@exemple.fr");
        assertThat(c.getVerificationInstructions())
                .isEqualTo("Reprendre 30 pieces au calibre apres 2 semaines de production.");
    }

    @Test
    void verification_exigee_sans_verificateur_est_refusee() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        // Une verification que personne ne doit faire n'aura pas lieu : mieux vaut
        // la refuser que d'enregistrer une intention sans titulaire.
        assertThatThrownBy(() -> service.updateCase(c.getId(),
                verification(true, null, null, "Controler le couple de serrage.")))
                .isInstanceOf(CapaValidationException.class);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void ne_plus_exiger_la_verification_efface_ce_qui_n_a_plus_d_objet() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setVerificationRequired(true);
        c.setVerificationAssigneeId(UUID.randomUUID());
        c.setVerificationAssigneeName("controle@exemple.fr");
        c.setVerificationInstructions("Reprendre 30 pieces.");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.updateCase(c.getId(), verification(false, null, null, null));

        // Laisser trainer un verificateur laisserait croire qu'une verification
        // est encore attendue.
        assertThat(c.getVerificationRequired()).isFalse();
        assertThat(c.getVerificationAssigneeId()).isNull();
        assertThat(c.getVerificationAssigneeName()).isNull();
        assertThat(c.getVerificationInstructions()).isNull();
    }

    @Test
    void designer_un_verificateur_sans_exiger_la_verification_est_refuse() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        // C'est presque toujours une case oubliee ; l'accepter en silence
        // produirait un dossier qui se croit couvert.
        assertThatThrownBy(() -> service.updateCase(c.getId(),
                verification(null, UUID.randomUUID(), "controle@exemple.fr", null)))
                .isInstanceOf(CapaValidationException.class);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void une_mise_a_jour_muette_ne_touche_pas_a_la_verification() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        UUID verificateur = UUID.randomUUID();
        c.setVerificationRequired(true);
        c.setVerificationAssigneeId(verificateur);
        c.setVerificationAssigneeName("controle@exemple.fr");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        // La mise a jour est PARTIELLE : ne rien dire n'est pas dire « non ».
        service.updateCase(c.getId(), new CapaDto.UpdateCaseRequest(
                "nouveau titre", null, null, null, null, null, null, null, null, null, null, null));

        assertThat(c.getTitle()).isEqualTo("nouveau titre");
        assertThat(c.getVerificationRequired()).isTrue();
        assertThat(c.getVerificationAssigneeId()).isEqualTo(verificateur);
    }

    @Test
    void les_consignes_seules_se_modifient_sur_un_dossier_deja_exige() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setVerificationRequired(true);
        c.setVerificationAssigneeId(UUID.randomUUID());
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.updateCase(c.getId(), verification(null, null, null, "Nouvelle consigne."));

        assertThat(c.getVerificationInstructions()).isEqualTo("Nouvelle consigne.");
    }

    // --- le statut du dossier se deduit de ses actions (ADR 0117) ---

    /**
     * Commencer la premiere action met le dossier en traitement, sans qu'un
     * second geste soit demande. C'est le defaut que l'ADR 0117 corrige : le
     * pilote devait cliquer « Demarrer » pour annoncer un travail dont son
     * action temoignait deja, et l'oubliait.
     */
    @Test
    void commencerUneAction_metLeDossierEnTraitement() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);
        when(caseRepo.save(c)).thenReturn(c);

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null, null, null, null, null, null));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    /** Terminer la DERNIERE action resout le dossier, et l'horodate. */
    @Test
    void terminerLaDerniereAction_resoutLeDossier() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        action(c, CapaActionStatus.DONE);
        CapaAction derniere = action(c, CapaActionStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(derniere.getId(), c.getId()))
                .thenReturn(Optional.of(derniere));
        when(actionRepo.save(derniere)).thenReturn(derniere);
        when(caseRepo.save(c)).thenReturn(c);

        service.updateAction(c.getId(), derniere.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.DONE, null, null, null, null, null, null, null, null, null, null));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        assertThat(c.getResolvedAt()).isNotNull();
    }

    /**
     * Tant qu'UNE action reste a faire, le dossier n'est pas resolu -- c'est le
     * verrou que l'ancien /resolve opposait, et qu'on garde en le rendant
     * automatique plutot qu'en le faisant demander.
     */
    @Test
    void uneActionInachevee_empecheLaResolution() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        action(c, CapaActionStatus.PENDING);
        CapaAction autre = action(c, CapaActionStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(autre.getId(), c.getId())).thenReturn(Optional.of(autre));
        when(actionRepo.save(autre)).thenReturn(autre);

        service.updateAction(c.getId(), autre.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.DONE, null, null, null, null, null, null, null, null, null, null));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    /**
     * Rouvrir une action d'un dossier resolu le ramene en traitement, efface
     * l'horodatage de resolution -- la fiche dirait sinon a la fois « en
     * cours » et « resolu le... » -- et s'inscrit au journal comme un RETOUR,
     * jamais comme un echec de verification.
     */
    @Test
    void rouvrirUneAction_rameneLeDossierEnTraitement_etLeConsigne() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        CapaAction a = action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);
        when(caseRepo.save(c)).thenReturn(c);

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null, null, null, null, null, null));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        assertThat(c.getResolvedAt()).isNull();
        verify(journal).record(c, CapaTransition.REOPENED);
    }

    /**
     * Un dossier SANS action reste ouvert. « Aucune action inachevee » n'est
     * pas « le travail est fait » : resoudre les dossiers vides cloturerait
     * exactement ceux qui attendent qu'on ecrive quoi faire.
     */
    @Test
    void dossierSansAction_resteOuvert() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addAction(c.getId(), new CapaDto.ActionRequest(
                "a faire", null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.OPEN);
        // Rien n'a change : le dossier n'est pas reenregistre pour autant.
        verify(caseRepo, never()).save(any());
    }

    /**
     * Ajouter une action a un dossier resolu le ramene en traitement : il reste
     * quelque chose a faire, et c'est le cas d'une efficacite partielle qu'on
     * complete (ADR 0108).
     */
    @Test
    void ajouterUneAction_aUnDossierResolu_leRamemeEnTraitement() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseRepo.save(c)).thenReturn(c);

        service.addAction(c.getId(), new CapaDto.ActionRequest(
                "complement", null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        assertThat(c.getResolvedAt()).isNull();
    }

    /**
     * Un dossier CLOS ou REJETE ne bouge plus : la cloture repose sur un
     * verdict d'efficacite humain (ADR 0108) et l'abandon sur une decision.
     * Aucune ecriture d'action ne les defait.
     */
    @Test
    void dossierClos_neRepartPasParSesActions() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        CapaAction a = action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest(null, null, CapaActionStatus.PENDING,
                        null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
    }

    // --- effectiveness ---
    @Test
    void effectiveness_effective_closesCapa() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(true));
        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
        assertThat(c.getEffectivenessVerified()).isTrue();
        assertThat(c.getClosedAt()).isNotNull();
    }

    @Test
    void effectiveness_notEffective_reopensInProgress() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));
        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        assertThat(c.getEffectivenessVerified()).isFalse();
        assertThat(c.getResolvedAt()).isNull();
    }

    @Test
    void effectiveness_notResolved_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- cloture directe : aucune verification n'a ete promise ---

    /**
     * La question se tranche a l'ouverture, et elle engage : repondue « non »,
     * ou jamais posee, la cloture ne doit pas reclamer un verdict. L'ecran
     * n'offrait que « Efficace -- cloturer », et inscrivait donc au registre
     * une efficacite que personne n'avait constatee.
     */
    @Test
    void closeWithoutVerification_noPromise_closesAndJudgesNothing() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setVerificationRequired(false);
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.closeWithoutVerification(c.getId());

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
        assertThat(c.getClosedAt()).isNotNull();
        // NI vrai NI faux : personne n'a juge, et le registre ne doit pas
        // laisser croire le contraire.
        assertThat(c.getEffectivenessVerified()).isNull();
    }

    /**
     * L'ENDIGUEMENT SE CLOT PAR ICI, et seulement par ici. Depuis l'ADR 0135
     * chaque action porte son dossier : le refus oppose par verifyEffectiveness
     * a un dossier qui n'endigue que le rendait infermable pour toujours.
     */
    @Test
    void closeWithoutVerification_containmentOnly_closes() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.closeWithoutVerification(c.getId());

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
    }

    /** Une promesse de verification ne se contourne pas : elle se solde. */
    @Test
    void closeWithoutVerification_casePromised_throws() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setVerificationRequired(true);
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.closeWithoutVerification(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void closeWithoutVerification_actionPromised_throws() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        CapaAction a = action(c, CapaActionStatus.DONE);
        a.setVerificationRequired(true);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.closeWithoutVerification(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    /** Un critere annonce dit ce qu'on mesurera : il se solde par un verdict. */
    @Test
    void closeWithoutVerification_criterionAnnounced_throws() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setEffectivenessCriterion("rebuts < 2 % sur 3 mois");
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.closeWithoutVerification(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    /** Les autres obstacles de la cloture restent opposes par ce chemin. */
    @Test
    void closeWithoutVerification_actionNotDone_throws() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.closeWithoutVerification(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void closeWithoutVerification_notResolved_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.closeWithoutVerification(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    // --- reject ---
    @Test
    void reject_open_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.rejectCase(c.getId());
        assertThat(c.getStatus()).isEqualTo(CapaStatus.REJECTED);
    }

    @Test
    void reject_inProgress_success() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        service.rejectCase(c.getId());
        assertThat(c.getStatus()).isEqualTo(CapaStatus.REJECTED);
    }

    @Test
    void reject_resolved_throws() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.rejectCase(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    // --- delete ---
    @Test
    void delete_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        service.deleteCase(c.getId());
        verify(caseRepo).delete(c);
    }

    @Test
    void delete_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.deleteCase(c.getId()))
                .isInstanceOf(CapaStateException.class);
    }

    // --- addAction ---
    @Test
    void addAction_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> {
            CapaAction a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });
        CapaDto.ActionRequest req = new CapaDto.ActionRequest(
                "fix", "desc", null, null, UUID.randomUUID(), null, null, LocalDate.now().plusDays(5), null, null, null, null, null);
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.PENDING);
    }

    /**
     * UNE ACTION NEE FAITE EST FAITE A CET INSTANT.
     *
     * <p>La date d'achevement n'etait posee qu'a la mise a jour. Une action
     * creee directement « Faite » -- une reprise consignee apres coup, un geste
     * deja mene quand on ouvre le dossier -- portait donc l'etat sans la date,
     * et les deux champs du meme fait se contredisaient dans le meme
     * enregistrement. Tout ce qui se fie a la date l'ignorait : le D6 du
     * rapport 8D la declarait jamais menee sous un D5 qui l'affichait faite.
     */
    @Test
    void addAction_creeeFaite_porteSaDateDAchevement() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse faite = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Trier le lot", null, CapaActionStatus.DONE, CapaActionType.CONTAINMENT,
                null, null, null, null, null, null, null, null, null));

        assertThat(faite.status()).isEqualTo(CapaActionStatus.DONE);
        assertThat(faite.completedAt()).isNotNull();
    }

    /** Et une action qui reste a faire n'en porte aucune. */
    @Test
    void addAction_creeeAFaire_nAPasDeDateDAchevement() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse aFaire = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Reprendre la gamme", null, null, null,
                null, null, null, null, null, null, null, null, null));

        assertThat(aFaire.completedAt()).isNull();
    }

    /**
     * Un dossier porte PLUSIEURS actions (ADR 0115, qui revise l'ADR 0088 sur
     * ce seul point), quelle que soit sa source.
     *
     * <p>Un ecart reel se traite rarement d'un seul geste : on endigue, on
     * corrige la cause, on previent la recidive -- et le 8D nomme ces etapes
     * separement (D3, D5, D6). La regle « une seule action » obligeait a ouvrir
     * un second dossier sur le meme ecart, ou a tout ecrire dans un intitule
     * unique : dans les deux cas, l'avancement n'etait plus mesurable action
     * par action.
     */
    @Test
    void addAction_dossierAyantDejaUneAction_enAccepteUneSeconde_quelleQueSoitSaSource() {
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        for (CapaSourceType source : CapaSourceType.values()) {
            CapaCase c = capa(TENANT, CapaStatus.OPEN);
            c.setSourceType(source);
            action(c, CapaActionStatus.PENDING);
            when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

            CapaDto.ActionResponse seconde = service.addAction(c.getId(), new CapaDto.ActionRequest(
                    "Reprendre le reglage de la presse", null, null,
                    CapaActionType.PREVENTIVE, null, null, null, null, null, null, null, null, null));

            assertThat(seconde.title()).as("source %s", source)
                    .isEqualTo("Reprendre le reglage de la presse");
            assertThat(seconde.actionType()).isEqualTo(CapaActionType.PREVENTIVE);
        }
    }

    /**
     * Le pilote du dossier est celui de la PREMIERE action. Les suivantes ne le
     * deplacent pas : la troisieme action aurait sinon transfere le pilotage a
     * qui l'execute, alors que c'est au pilote qu'on demande des nouvelles.
     */
    @Test
    void addAction_secondeAction_neDeplacePasLePiloteDuDossier() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Former l'equipe de nuit", null, null, null, UUID.randomUUID(),
                "Autre personne", null, null, null, null, null, null, null));

        assertThat(c.getOwnerId()).isEqualTo(OWNER);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void addAction_leResponsableDeLaPremiereActionDevientLePiloteDuDossier() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        UUID responsable = UUID.randomUUID();
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Définir le plan", null, null, null, responsable, "  a.diallo@client.test  ", null, null, null, null, null, null, null));

        assertThat(c.getOwnerId()).isEqualTo(responsable);
        assertThat(c.getOwnerName()).isEqualTo("a.diallo@client.test");
        verify(caseRepo).save(c);
    }

    @Test
    void addAction_sansResponsableDesigne_laissePiloteEnPlace() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Définir le plan", null, null, null, null, "Nom libre", null, null, null, null, null, null, null));

        assertThat(c.getOwnerId()).isEqualTo(OWNER);
        verify(caseRepo, never()).save(any());
    }

    // --- openCase : le dossier ET son action, en une écriture (ADR 0088) ---

    @Test
    void openCase_creeLeDossierEtSonActionUnique_confieeAuPilote() {
        UUID pilote = UUID.randomUUID();
        when(caseRepo.save(any())).thenAnswer(inv -> {
            CapaCase c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        LocalDate echeance = LocalDate.now().plusDays(30);

        CapaDto.CaseResponse r = service.openCase(new CapaDto.OpenCaseRequest(
                "  Recalibrer la sonde  ", "  Dérive constatée  ", CapaType.PREVENTIVE,
                CapaCriticity.HIGH, CapaSourceType.OTHER, "ignoré", pilote, " a.diallo@client.test ",
                echeance, null, null, null, null), CapaSourceType.NON_CONFORMITY, " NC-2026-0001 ");

        ArgumentCaptor<CapaCase> saved = ArgumentCaptor.forClass(CapaCase.class);
        verify(caseRepo).save(saved.capture());
        CapaCase c = saved.getValue();
        assertThat(c.getTenantId()).isEqualTo(TENANT);
        assertThat(c.getTitle()).isEqualTo("Recalibrer la sonde");
        assertThat(c.getDescription()).isEqualTo("Dérive constatée");
        // La source est celle fixée par l'appelant serveur, jamais celle du corps.
        assertThat(c.getSourceType()).isEqualTo(CapaSourceType.NON_CONFORMITY);
        assertThat(c.getSourceRef()).isEqualTo("NC-2026-0001");
        assertThat(c.getOwnerId()).isEqualTo(pilote);
        assertThat(c.getOwnerName()).isEqualTo("a.diallo@client.test");
        assertThat(c.getStatus()).isEqualTo(CapaStatus.OPEN);
        assertThat(c.getDueDate()).isEqualTo(echeance);
        assertThat(c.getActions()).singleElement().satisfies(a -> {
            assertThat(a.getTitle()).isEqualTo("Recalibrer la sonde");
            assertThat(a.getActionType()).isEqualTo(CapaActionType.PREVENTIVE);
            assertThat(a.getAssigneeId()).isEqualTo(pilote);
            assertThat(a.getAssigneeName()).isEqualTo("a.diallo@client.test");
            assertThat(a.getStatus()).isEqualTo(CapaActionStatus.PENDING);
            assertThat(a.getDecidedOn()).isEqualTo(LocalDate.now());
            assertThat(a.getDueDate()).isEqualTo(echeance);
            assertThat(a.getCapa()).isSameAs(c);
        });
        assertThat(r.ownerName()).isEqualTo("a.diallo@client.test");
        assertThat(r.actions()).hasSize(1);
        verify(journal).record(c, CapaTransition.OPENED);
        // Une seule écriture : l'action part par cascade avec le dossier.
        verify(actionRepo, never()).save(any());
    }

    @Test
    void openCase_correctif_donneUneActionCorrective_etUnNomBlancDevientNul() {
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CapaDto.CaseResponse r = service.openCase(openReq(CapaType.CORRECTIVE, LocalDate.now(), "   "),
                CapaSourceType.INTERNAL, null);
        assertThat(r.actions().get(0).actionType()).isEqualTo(CapaActionType.CORRECTIVE);
        assertThat(r.ownerName()).isNull();
        assertThat(r.sourceRef()).isNull();
    }

    @Test
    void openCase_refuseLEndiguementSeul_quiNeSeCloraitJamais() {
        assertThatThrownBy(() -> service.openCase(openReq(CapaType.CONTAINMENT, LocalDate.now(), null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("containment");
        verifyNoInteractions(caseRepo);
    }

    @Test
    void openCase_refuseUneEcheancePassee() {
        assertThatThrownBy(() -> service.openCase(openReq(CapaType.CORRECTIVE, LocalDate.now().minusDays(1), null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("past");
        verifyNoInteractions(caseRepo);
    }

    @Test
    void openCase_refuseLesChampsManquants() {
        LocalDate d = LocalDate.now().plusDays(3);
        UUID o = UUID.randomUUID();
        assertThatThrownBy(() -> service.openCase(null, CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class);
        assertThatThrownBy(() -> service.openCase(new CapaDto.OpenCaseRequest(
                "t", null, null, CapaCriticity.LOW, null, null, o, null, d, null, null, null, null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class);
        assertThatThrownBy(() -> service.openCase(new CapaDto.OpenCaseRequest(
                "t", null, CapaType.CORRECTIVE, null, null, null, o, null, d, null, null, null, null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class).hasMessageContaining("Criticity");
        assertThatThrownBy(() -> service.openCase(new CapaDto.OpenCaseRequest(
                "t", null, CapaType.CORRECTIVE, CapaCriticity.LOW, null, null, null, null, d, null, null, null, null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class).hasMessageContaining("entrusted");
        assertThatThrownBy(() -> service.openCase(new CapaDto.OpenCaseRequest(
                "t", null, CapaType.CORRECTIVE, CapaCriticity.LOW, null, null, o, null, null, null, null, null, null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class).hasMessageContaining("due date");
        assertThatThrownBy(() -> service.openCase(openReq(CapaType.CORRECTIVE, d, null), null, null))
                .isInstanceOf(CapaValidationException.class).hasMessageContaining("source");
        assertThatThrownBy(() -> service.openCase(new CapaDto.OpenCaseRequest(
                "   ", null, CapaType.CORRECTIVE, CapaCriticity.LOW, null, null, o, null, d, null, null, null, null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class).hasMessageContaining("blank");
        assertThatThrownBy(() -> service.openCase(openReq(CapaType.CORRECTIVE, d, "x".repeat(256)),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class).hasMessageContaining("Assignee name");
        verifyNoInteractions(caseRepo);
    }

    @Test
    void openCase_sansTenant_estRefuse() {
        TenantContext.clear();
        assertThatThrownBy(() -> service.openCase(openReq(CapaType.CORRECTIVE, LocalDate.now(), null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(caseRepo);
    }

    // --- openCase : la décision de vérification part avec l'ouverture (ADR 0073, 0088) ---

    @Test
    void openCase_verificationExigee_estPoseeAvecSonVerificateurEtSesConsignes() {
        UUID verificateur = UUID.randomUUID();
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.CaseResponse r = service.openCase(openReqVerif(true, verificateur,
                        "  Claire Contrôle  ", "  Relever 20 pièces  "),
                CapaSourceType.NON_CONFORMITY, "NC-2026-0001");

        assertThat(r.verificationRequired()).isTrue();
        assertThat(r.verificationAssigneeId()).isEqualTo(verificateur);
        assertThat(r.verificationAssigneeName()).isEqualTo("Claire Contrôle");
        assertThat(r.verificationInstructions()).isEqualTo("Relever 20 pièces");
    }

    @Test
    void openCase_verificationNonExigee_estUneDecisionQuiSeLit() {
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.CaseResponse r = service.openCase(openReqVerif(false, null, null, null),
                CapaSourceType.AUDIT, "AUD-2026-0003 / constat 2");

        // FALSE n'est pas null : le dossier a été jugé non vérifiable, délibérément.
        assertThat(r.verificationRequired()).isFalse();
        assertThat(r.verificationAssigneeId()).isNull();
        assertThat(r.verificationInstructions()).isNull();
    }

    @Test
    void openCase_sansDecision_laisseLaQuestionOuverte() {
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.CaseResponse r = service.openCase(openReq(CapaType.CORRECTIVE, LocalDate.now(), null),
                CapaSourceType.INTERNAL, null);

        assertThat(r.verificationRequired()).isNull();
    }

    @Test
    void openCase_exigerSansDesigner_estRefuse_sansRienEcrire() {
        assertThatThrownBy(() -> service.openCase(openReqVerif(true, null, null, "Relever"),
                CapaSourceType.NON_CONFORMITY, "NC-2026-0001"))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("assigned");
        verifyNoInteractions(caseRepo);
        verifyNoInteractions(journal);
    }

    @Test
    void openCase_designerSansExiger_estRefuse() {
        assertThatThrownBy(() -> service.openCase(openReqVerif(null, UUID.randomUUID(), "X", null),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("verificationRequired=true");
        assertThatThrownBy(() -> service.openCase(openReqVerif(null, null, null, "Relever"),
                CapaSourceType.INTERNAL, null))
                .isInstanceOf(CapaValidationException.class);
        verifyNoInteractions(caseRepo);
    }

    private static CapaDto.OpenCaseRequest openReqVerif(Boolean exigee, UUID qui, String nom, String consignes) {
        return new CapaDto.OpenCaseRequest("Recalibrer", null, CapaType.CORRECTIVE, CapaCriticity.MEDIUM,
                null, null, OWNER, "Pilote", LocalDate.now().plusDays(5), exigee, qui, nom, consignes);
    }

    private static CapaDto.OpenCaseRequest openReq(CapaType type, LocalDate due, String ownerName) {
        return new CapaDto.OpenCaseRequest("Recalibrer", null, type, CapaCriticity.MEDIUM,
                null, null, OWNER, ownerName, due, null, null, null, null);
    }

    @Test
    void addAction_dossierSansAction_recoitLaSienne() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setSourceType(CapaSourceType.RISK);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CapaDto.ActionRequest req = new CapaDto.ActionRequest(
                "unique", null, null, null, null, null, null, null, null, null, null, null, null);
        assertThat(service.addAction(c.getId(), req).title()).isEqualTo("unique");
    }

    @Test
    void addAction_explicitStatus_used() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CapaDto.ActionRequest req = new CapaDto.ActionRequest(
                "fix", null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null, null, null, null, null, null);
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.IN_PROGRESS);
    }

    @Test
    void addAction_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- updateAction ---
    @Test
    void updateAction_markDone_setsCompletedAt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest("t", null, CapaActionStatus.DONE, null, null, null, null, null, null, null, null, null, null));

        assertThat(a.getStatus()).isEqualTo(CapaActionStatus.DONE);
        assertThat(a.getCompletedAt()).isNotNull();
    }

    @Test
    void updateAction_revertFromDone_clearsCompletedAt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.DONE);
        a.setCompletedAt(Instant.now());
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest(null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null, null, null, null, null, null));

        assertThat(a.getStatus()).isEqualTo(CapaActionStatus.IN_PROGRESS);
        assertThat(a.getCompletedAt()).isNull();
    }

    @Test
    void updateAction_updatesFields() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        UUID assignee = UUID.randomUUID();
        LocalDate due = LocalDate.now().plusDays(3);
        service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest("nt", "nd", null, null, assignee, null, null, due, null, null, null, null, null));
        assertThat(a.getTitle()).isEqualTo("nt");
        assertThat(a.getDescription()).isEqualTo("nd");
        assertThat(a.getAssigneeId()).isEqualTo(assignee);
        assertThat(a.getDueDate()).isEqualTo(due);
    }

    @Test
    void updateAction_notFound_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        UUID actionId = UUID.randomUUID();
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(actionId, c.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateAction(c.getId(), actionId,
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaActionNotFoundException.class);
    }

    @Test
    void updateAction_capaClosed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateAction(c.getId(), UUID.randomUUID(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- deleteAction ---
    @Test
    void deleteAction_success() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        service.deleteAction(c.getId(), a.getId());
        verify(actionRepo).delete(a);
    }

    @Test
    void deleteAction_capaRejected_throws() {
        CapaCase c = capa(TENANT, CapaStatus.REJECTED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.deleteAction(c.getId(), UUID.randomUUID()))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void deleteAction_notFound_throws() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        UUID aid = UUID.randomUUID();
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(aid, c.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteAction(c.getId(), aid))
                .isInstanceOf(CapaActionNotFoundException.class);
    }

    // --- helpers ---
    private CapaDto.CreateCaseRequest req() {
        return new CapaDto.CreateCaseRequest(
                "Défaut soudure", "desc", CapaType.CORRECTIVE, CapaCriticity.HIGH,
                CapaSourceType.NON_CONFORMITY, "NC-001", OWNER, null, LocalDate.now().plusDays(30));
    }

    // --- verrou de clôture sur les non-conformités liées ------------------------
    // Clore une CAPA au-dessus d'un écart encore ouvert reviendrait à déclarer le
    // problème réglé pendant que le constat dit le contraire.

    @Test
    void refuse_deCloreQuandUneNcLieeResteOuverte() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(eq(TENANT), eq(c.getId()), any()))
                .thenReturn(2L);

        assertThatThrownBy(() -> service.verifyEffectiveness(
                c.getId(), new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("2");

        // Rien n'a bougé : ni le statut, ni la date de clôture, ni le journal.
        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        assertThat(c.getClosedAt()).isNull();
        verifyNoInteractions(journal);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void cloture_quandToutesLesNcLieesSontRefermees() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(eq(TENANT), eq(c.getId()), any()))
                .thenReturn(0L);
        when(caseRepo.save(any())).thenReturn(c);

        CapaDto.CaseResponse r = service.verifyEffectiveness(
                c.getId(), new CapaDto.EffectivenessRequest(true));

        assertThat(r.status()).isEqualTo(CapaStatus.CLOSED);
    }

    @Test
    void neVerrouillePas_uneEfficaciteNonDemontree() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        // Le dossier repart en traitement : il ne se referme pas, donc le verrou
        // ne s'applique pas — et c'est CELA que le test surveille.
        //
        // Il ne peut plus le surveiller en exigeant zéro appel au dépôt des NC :
        // le même décompte sert désormais à ÉNONCER les obstacles restants sur la
        // fiche renvoyée, ce qui est un usage tout différent du verrou. Compter
        // les appels confondrait « le verrou s'est appliqué » avec « l'écran sait
        // quoi afficher ». Le statut, lui, ne dit qu'une chose.
        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        assertThat(c.getClosedAt()).isNull();
    }

    // --- journal du cycle de vie (§11.5) ---------------------------------------
    // Le dossier consignait le versement d'une preuve mais pas la décision qui
    // clôt le cas. Un dossier d'audit qui dit QUI a joint une pièce sans dire QUI
    // l'a clos raconte l'accessoire et tait l'essentiel.

    @Test
    void ouverture_estConsignee() {
        CapaCase saved = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.save(any())).thenReturn(saved);

        service.createCase(req());

        verify(journal).record(saved, CapaTransition.OPENED);
    }

    @Test
    void demarrage_estConsigne() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);
        when(caseRepo.save(any())).thenReturn(c);

        // Le demarrage n'est plus demande, il est CONSTATE : commencer la
        // premiere action est l'acte, et c'est lui qu'on consigne (ADR 0117).
        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null, null, null, null, null, null));

        verify(journal).record(c, CapaTransition.STARTED);
    }

    @Test
    void resolution_estConsignee() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);
        when(caseRepo.save(any())).thenReturn(c);

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.DONE, null, null, null, null, null, null, null, null, null, null));

        verify(journal).record(c, CapaTransition.RESOLVED);
    }

    @Test
    void cloture_surEfficaciteDemontree_estConsignee() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(true));

        verify(journal).record(c, CapaTransition.CLOSED);
    }

    @Test
    void efficaciteNonDemontree_estConsigneeCommeTelle() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        // Le dossier repart en traitement : dire « clos » ici serait faux, et
        // taire l'événement masquerait une action corrective sans effet.
        verify(journal).record(c, CapaTransition.EFFECTIVENESS_REJECTED);
    }

    @Test
    void rejet_estConsigne() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenReturn(c);

        service.rejectCase(c.getId());

        verify(journal).record(c, CapaTransition.REJECTED);
    }

    @Test
    void suppression_estConsigneeAvantEffacement() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        service.deleteCase(c.getId());

        // L'ordre compte : après l'effacement, il ne reste rien à décrire.
        InOrder order = inOrder(journal, caseRepo);
        order.verify(journal).record(c, CapaTransition.DELETED);
        order.verify(caseRepo).delete(c);
    }

    @Test
    void unRefusNeLaisseAucuneTrace() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        CapaAction a = action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest(null, null, CapaActionStatus.PENDING,
                        null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);

        // Une transition refusée n'a pas eu lieu : la consigner mentirait.
        verifyNoInteractions(journal);
    }

    // --- colonnes du tableau des actions (ADR 0052) ----------------------------
    // Ce qui se teste ici n'est pas « le champ est-il stocké » — c'est que chaque
    // colonne dit ce qu'elle prétend dire : la date est celle de la DÉCISION et
    // non de la saisie, le nom du porteur est celui figé à la décision, et le
    // libellé ne peut pas être vidé par une édition en ligne.

    @Test
    void addAction_dateDeDecisionSaisie_estConservee() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LocalDate comite = LocalDate.of(2026, 3, 12);

        CapaDto.ActionResponse r = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Réviser le plan de contrôle", null, null, null, null, "Amina Dridi", comite, null, null, null, null, null, null));

        // Une action décidée en comité le 12 mars et saisie plus tard porte le
        // 12 mars : c'est toute la raison d'être de la colonne.
        assertThat(r.decidedOn()).isEqualTo(comite);
        assertThat(r.assigneeName()).isEqualTo("Amina Dridi");
    }

    @Test
    void addAction_sansDateDeDecision_deduitLeJour_maisPasDepuisCreatedAt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null, null, null, null, null, null));

        // Déduction EXPLICITE au jour de l'enregistrement, et corrigeable ensuite.
        // Le champ existe indépendamment de createdAt : l'entité l'a reçu avant
        // toute persistance, là où createdAt n'est posé qu'au @PrePersist.
        assertThat(r.decidedOn()).isEqualTo(LocalDate.now());
        assertThat(r.createdAt()).isNull();
    }

    @Test
    void addAction_nomDePorteurVide_estRamèneANull_plutotQueDAfficherDuBlanc() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null, "   ", null, null, null, null, null, null, null));

        // Sinon la colonne « Responsable » serait vide sans le « — » qui dit
        // qu'elle l'est : l'écran n'aurait plus qu'une seule chose à tester.
        assertThat(r.assigneeName()).isNull();
    }

    @Test
    void addAction_titreFaitDEspaces_estRefuse() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("   ", null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaValidationException.class);
        verify(actionRepo, never()).save(any());
    }

    @Test
    void addAction_titreTropLong_estRefuseSansTronquer() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("x".repeat(256), null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("255");
        // Tronquer serait pire : l'utilisateur croirait avoir enregistré ce
        // qu'il a tapé.
        verify(actionRepo, never()).save(any());
    }

    @Test
    void updateAction_editionEnLigne_changeLeLibelleEtLeStatut() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest("  Libellé corrigé  ", null,
                        CapaActionStatus.IN_PROGRESS, null, null, null, null, null, null, null, null, null, null));

        // Les espaces de bordure d'un champ de tableau viennent du copier-coller,
        // pas de l'intention : les garder ferait diverger deux libellés identiques.
        assertThat(r.title()).isEqualTo("Libellé corrigé");
        assertThat(r.status()).isEqualTo(CapaActionStatus.IN_PROGRESS);
    }

    @Test
    void updateAction_libelleVide_estRefuse_etNeVidePasLAction() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest(" ", null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(CapaValidationException.class);

        // Le PATCH n'est pas validé par Jakarta (un champ absent doit rester
        // intouché) : sans le garde-fou du service, l'édition en ligne effacerait
        // le libellé sur un simple espace.
        assertThat(a.getTitle()).isEqualTo("act");
        verify(actionRepo, never()).save(any());
    }

    @Test
    void updateAction_neTouchePasCeQuiNEstPasEnvoye() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        a.setAssigneeName("Amina Dridi");
        a.setDecidedOn(LocalDate.of(2026, 3, 12));
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, CapaActionStatus.DONE, null, null, null, null, null, null, null, null, null, null));

        assertThat(a.getAssigneeName()).isEqualTo("Amina Dridi");
        assertThat(a.getDecidedOn()).isEqualTo(LocalDate.of(2026, 3, 12));
    }

    @Test
    void deleteAction_refuseDEffacerUneActionQuiPorteUnePreuve() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(evidenceRepo.countByTenantIdAndActionId(TENANT, a.getId())).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteAction(c.getId(), a.getId()))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("remove it before deleting the action");

        // La cascade de base effacerait la ligne mais laisserait le binaire
        // orphelin — et surtout, une preuve d'audit disparaîtrait sans trace.
        verify(actionRepo, never()).delete(any());
    }

    @Test
    void deleteAction_effaceUneActionSansPreuve() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(evidenceRepo.countByTenantIdAndActionId(TENANT, a.getId())).thenReturn(0L);

        service.deleteAction(c.getId(), a.getId());

        verify(actionRepo).delete(a);
    }

    // --- non-conformité d'origine ------------------------------------------------

    @Test
    void findById_porteLaNonConformiteDOrigine_parSonLienReel() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.of(nc("NC-2026-0018", "Étiquetage lot 4471 illisible")));

        CapaDto.CaseResponse r = service.findById(c.getId());

        assertThat(r.sourceNonConformity()).isNotNull();
        assertThat(r.sourceNonConformity().reference()).isEqualTo("NC-2026-0018");
        assertThat(r.sourceNonConformity().title()).isEqualTo("Étiquetage lot 4471 illisible");
    }

    @Test
    void findById_retombeSurLaReferenceSaisie_quandAucuneNcNePointeVersLeDossier() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setSourceRef(" NC-2026-0018 ");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.empty());
        when(ncRepo.findByTenantIdAndReference(TENANT, "NC-2026-0018"))
                .thenReturn(Optional.of(nc("NC-2026-0018", "Étiquetage lot 4471 illisible")));

        assertThat(service.findById(c.getId()).sourceNonConformity().title())
                .isEqualTo("Étiquetage lot 4471 illisible");
    }

    @Test
    void findById_neMontreRien_quandLaReferenceSaisieNeDesigneAucunEcart() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setSourceRef("NC-INEXISTANTE");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.empty());
        when(ncRepo.findByTenantIdAndReference(TENANT, "NC-INEXISTANTE")).thenReturn(Optional.empty());

        // Une référence tapée à la main peut désigner un écart qui n'existe pas :
        // ne rien montrer vaut mieux qu'afficher un nom inventé.
        assertThat(service.findById(c.getId()).sourceNonConformity()).isNull();
    }

    @Test
    void findById_neChercheAucunEcart_quandLaSourceNEnEstPasUn() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setSourceType(CapaSourceType.AUDIT);
        c.setSourceRef("AUD-2026-Q2");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.findFirstByTenantIdAndCapaCaseIdOrderByDetectedAtAsc(TENANT, c.getId()))
                .thenReturn(Optional.empty());

        assertThat(service.findById(c.getId()).sourceNonConformity()).isNull();
        verify(ncRepo, never()).findByTenantIdAndReference(any(), any());
    }

    @Test
    void findAll_neResoutAucunEcart_pourNePasFaireUneRequeteParLigne() {
        Pageable p = PageRequest.of(0, 20);
        when(caseRepo.findByTenantId(TENANT, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.OPEN))));

        Page<CapaDto.CaseResponse> page = service.findAll(null, p);

        assertThat(page.getContent().get(0).sourceNonConformity()).isNull();
        // Vingt dossiers vaudraient vingt requêtes pour une colonne que la liste
        // n'affiche même pas.
        verifyNoInteractions(ncRepo);
    }

    private com.openlab.qualitos.quality.nonconformity.NonConformity nc(String ref, String title) {
        var n = new com.openlab.qualitos.quality.nonconformity.NonConformity();
        n.setId(UUID.randomUUID());
        n.setTenantId(TENANT);
        n.setReference(ref);
        n.setTitle(title);
        return n;
    }

    private CapaCase capa(UUID tenant, CapaStatus status) {
        CapaCase c = new CapaCase();
        c.setId(UUID.randomUUID());
        c.setTenantId(tenant);
        c.setTitle("t");
        c.setType(CapaType.CORRECTIVE);
        c.setCriticity(CapaCriticity.HIGH);
        c.setStatus(status);
        c.setSourceType(CapaSourceType.NON_CONFORMITY);
        c.setOwnerId(OWNER);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private CapaAction action(CapaCase c, CapaActionStatus status) {
        return action(c, status, CapaActionType.CORRECTIVE);
    }

    private CapaAction action(CapaCase c, CapaActionStatus status, CapaActionType type) {
        CapaAction a = new CapaAction();
        a.setId(UUID.randomUUID());
        a.setCapa(c);
        a.setTitle("act");
        a.setStatus(status);
        a.setActionType(type);
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        // L'action REJOINT la collection du dossier : en base, le contexte de
        // persistance rend la meme instance des deux cotes, et le statut du
        // dossier se deduit desormais de cette collection (ADR 0117). Un banc
        // qui l'oublierait testerait un dossier sans action.
        c.getActions().add(a);
        return a;
    }

    // ============================================================================
    // Nature des actions : endiguement / correction / prévention (§4.2)
    // ============================================================================

    @Test
    void addAction_withoutType_isCorrective() {
        // C'est ce que « une action de CAPA » veut dire par défaut ; exiger la
        // qualification à chaque saisie ferait payer tout le monde pour le cas rare.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(),
                new CapaDto.ActionRequest("tri du lot", null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(r.actionType()).isEqualTo(CapaActionType.CORRECTIVE);
    }

    @Test
    void addAction_withContainment_keepsIt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "tri du lot suspect", null, null, CapaActionType.CONTAINMENT,
                null, null, null, null, null, null, null, null, null));

        assertThat(r.actionType()).isEqualTo(CapaActionType.CONTAINMENT);
    }

    @Test
    void updateAction_absentType_leavesItUntouched() {
        // PATCH : un champ absent ne se touche pas. Sans ce comportement, corriger
        // une échéance requalifierait l'action en corrective.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, null, null, null, null, null, LocalDate.now().plusDays(3), null, null, null, null, null));

        assertThat(a.getActionType()).isEqualTo(CapaActionType.CONTAINMENT);
    }

    @Test
    void updateAction_newType_requalifiesTheAction() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        CapaAction a = action(c, CapaActionStatus.PENDING, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(a)).thenReturn(a);

        service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, null, CapaActionType.PREVENTIVE, null, null, null, null, null, null, null, null, null));

        assertThat(a.getActionType()).isEqualTo(CapaActionType.PREVENTIVE);
    }

    @Test
    void verifyEffectiveness_actionAddedAfterResolution_blocksClosure() {
        // Rien n'interdit d'ajouter une action à un dossier RESOLVED. Sans ce
        // contrôle, le dossier se cloturait au-dessus d'une action jamais menée —
        // et l'écran, qui annonce l'obstacle, interdisait un geste que l'API
        // accordait quand même.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE);
        action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("not DONE");

        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void verifyEffectiveness_pendingAction_stillAllowsAnIneffectiveVerdict() {
        // Constater l'échec ne clôt rien : l'interdire empêcherait de consigner
        // que les actions n'ont pas produit leur effet.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void verifyEffectiveness_containmentOnly_isRefused() {
        // Endiguer n'est pas corriger : la cause reste, et le problème reviendra
        // dès la mesure levée. Clore là-dessus inscrirait au registre le contraire
        // de ce qui s'est passé.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("containment");

        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void verifyEffectiveness_oneCorrectiveAmongContainments_closes() {
        // Une seule action qui s'attaque à la cause suffit : le dossier n'a pas
        // QUE contenu, et rien ne justifie d'exiger davantage.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        action(c, CapaActionStatus.DONE, CapaActionType.CORRECTIVE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(true));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
    }

    @Test
    void verifyEffectiveness_containmentOnly_butIneffective_isAllowed() {
        // Déclarer l'action INEFFICACE ne clôt rien : c'est un constat d'échec, et
        // le refuser interdirait de consigner que l'endiguement n'a pas suffi.
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void isContainmentOnly_emptyList_isFalse() {
        // Un dossier sans action a son propre motif ; un allMatch sur une liste
        // vide répondrait « oui » à une question qui ne se pose pas.
        assertThat(CapaService.isContainmentOnly(List.of())).isFalse();
    }

    // ============================================================================
    // Motifs de blocage, énoncés AVANT le clic (§4.2)
    // ============================================================================

    @Test
    void findById_caseWithoutAction_reportsNoActionBlocker() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .extracting(CapaDto.ClosureBlocker::code)
                .containsExactly(ClosureBlockerCode.NO_ACTION);
    }

    @Test
    void findById_pendingActions_areCounted() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        action(c, CapaActionStatus.DONE);
        action(c, CapaActionStatus.PENDING);
        action(c, CapaActionStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .containsExactly(new CapaDto.ClosureBlocker(ClosureBlockerCode.ACTIONS_NOT_DONE, 2));
    }

    @Test
    void findById_containmentOnly_isAnnouncedBeforeTheClick() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        // « N'a fait qu'endiguer » s'oppose a la DECLARATION d'une efficacite,
        // pas a la fermeture : l'obstacle ne vaut donc que sur le chemin du
        // verdict, celui d'un dossier qui a promis de verifier (ADR 0137).
        c.setVerificationRequired(true);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .containsExactly(new CapaDto.ClosureBlocker(ClosureBlockerCode.CONTAINMENT_ONLY, 2));
    }

    /**
     * LE BANC QUI MANQUAIT. L'ecran eteint « Cloturer » sur la liste d'obstacles
     * du serveur : si celle-ci annoncait encore CONTAINMENT_ONLY, le dossier
     * d'endiguement resterait infermable malgre son nouveau chemin.
     */
    @Test
    void findById_containmentOnlyWithoutPromise_isNoObstacle() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers()).isEmpty();
    }

    @Test
    void findById_openNonConformities_areCounted() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(3L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .containsExactly(new CapaDto.ClosureBlocker(ClosureBlockerCode.OPEN_NON_CONFORMITIES, 3));
    }

    @Test
    void findById_severalObstacles_areAllListed() {
        // L'écran doit pouvoir tout dire d'un coup : ne montrer que le premier
        // obstacle enverrait l'utilisateur en corriger un pour en découvrir un autre.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setVerificationRequired(true);
        action(c, CapaActionStatus.PENDING, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(1L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .extracting(CapaDto.ClosureBlocker::code)
                .containsExactly(ClosureBlockerCode.ACTIONS_NOT_DONE,
                        ClosureBlockerCode.CONTAINMENT_ONLY,
                        ClosureBlockerCode.OPEN_NON_CONFORMITIES);
    }

    @Test
    void findById_nothingInTheWay_returnsEmptyRatherThanNull() {
        // Une liste vide DIT quelque chose : « rien ne s'y oppose ». Un null
        // obligerait l'écran à distinguer « pas calculé » de « pas d'obstacle ».
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers()).isEmpty();
    }

    @Test
    void findById_closedCase_announcesNoObstacle() {
        // Afficher des obstacles sur un dossier terminé donnerait à croire qu'il
        // reste quelque chose à faire.
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThat(service.findById(c.getId()).closureBlockers()).isEmpty();
        // Et sans même interroger les non-conformités : la réponse est connue
        // d'avance, un dossier clos n'attend plus rien. (Le dépôt reste sollicité
        // par la résolution de l'écart d'origine, qui est un autre usage.)
        verify(ncRepo, never()).countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any());
    }

    @Test
    void findAll_doesNotComputeBlockers() {
        // Vingt dossiers vaudraient vingt requêtes pour une information que la
        // liste n'affiche pas.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByTenantId(eq(TENANT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(c)));

        assertThat(service.findAll(null, PageRequest.of(0, 20)).getContent().get(0).closureBlockers())
                .isNull();
        verifyNoInteractions(ncRepo);
    }

    // ============================================================================
    // Verdict d'efficacite a trois etats (ADR 0108)
    // ============================================================================
    //
    // Ce que ces tests protegent : le verdict PORTE ses consequences (clore, ou
    // repartir au travail), le critere est RECOPIE dans l'evaluation au moment du
    // jugement, et l'ancienne API continue de fonctionner a l'identique sans
    // jamais produire le troisieme etat.

    /** Identite authentifiee : l'auteur d'une evaluation se lit dans le jeton. */
    private static void connecte(UUID userId, String nom) {
        org.springframework.security.oauth2.jwt.Jwt jwt =
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("t")
                        .header("alg", "none").subject(userId.toString())
                        .claim("name", nom).build();
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(
                        new org.springframework.security.oauth2.server.resource.authentication
                                .JwtAuthenticationToken(jwt, List.of()));
    }

    @AfterEach
    void deconnecte() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private CapaCase aJuger(String critere) {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setEffectivenessCriterion(critere);
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        return c;
    }

    private static CapaDto.EffectivenessRequest verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict v, String releve) {
        return new CapaDto.EffectivenessRequest(null, v, "7,4 %", "< 2 %", releve, "mesure du mois");
    }

    private com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview captureEvaluation() {
        ArgumentCaptor<com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview> captor = ArgumentCaptor.forClass(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview.class);
        verify(reviews).save(captor.capture());
        return captor.getValue();
    }

    // ---------- un ecart d'objectif qualite ne se referme pas sur une declaration (ADR 0133) ----------

    /** Le retour sous la cible, tel que le module des objectifs le rapporte. */
    private void retourSousLaCible(boolean constate, int tenues, int exigees) {
        when(objectiveRecovery.forCase(any(), any())).thenReturn(Optional.of(
                new ObjectiveRecoveryPort.Recovery(constate, tenues, exigees,
                        "Le KPI revient a ou sous 500 ppm pendant 2 periode(s).")));
    }

    @Test
    void unDossierNeDUnObjectif_neSeClotPasAvantLeRetourDuKpiSousLaCible() {
        // Pour ce type de dossier, l'efficacite n'est pas une appreciation : elle
        // EST le retour du KPI sous la cible sur la duree definie. Le refus cite
        // les periodes tenues et celles qu'il faut, pour que le verificateur
        // sache combien de temps attendre plutot que de reessayer.
        CapaCase c = aJuger("Moins de 500 ppm sur 2 mois");
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);
        retourSousLaCible(false, 1, 2);

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE, "480 ppm")))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("quality objective")
                .hasMessageContaining("2 consecutive period(s)")
                .hasMessageContaining("1 have been observed")
                .hasMessageContaining("500 ppm");
        assertThat(c.getStatus()).isEqualTo(CapaStatus.RESOLVED);
        verify(reviews, never()).save(any());
    }

    @Test
    void leRetourConstate_laissePasserLeVerdictQuiClot() {
        CapaCase c = aJuger("Moins de 500 ppm sur 2 mois");
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);
        when(caseRepo.save(c)).thenReturn(c);
        retourSousLaCible(true, 2, 2);

        service.verifyEffectiveness(c.getId(),
                verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE, "470 ppm"));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
    }

    @Test
    void leVerrouNeVautQuePourUnVerdictQuiClot() {
        // Constater l'echec ne ferme rien : l'interdire empecherait d'ecrire que
        // les actions n'ont pas produit leur effet, et c'est ce constat dont la
        // boucle d'amelioration a besoin pour repartir.
        CapaCase c = aJuger("Moins de 500 ppm sur 2 mois");
        when(caseRepo.save(c)).thenReturn(c);
        retourSousLaCible(false, 0, 2);

        service.verifyEffectiveness(c.getId(),
                verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.INEFFECTIVE, "900 ppm"));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        verify(reviews).save(any());
    }

    @Test
    void unDossierOrdinaire_nEstPasConcerneParCeVerrou() {
        // Vide ne veut pas dire « pas encore revenu » : sans cette distinction,
        // tout dossier ordinaire deviendrait impossible a clore.
        CapaCase c = aJuger("Moins de 2 % de rebuts sur 3 mois");
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);
        when(caseRepo.save(c)).thenReturn(c);
        when(objectiveRecovery.forCase(any(), any())).thenReturn(Optional.empty());

        service.verifyEffectiveness(c.getId(),
                verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE, "1,1 %"));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
    }

    @Test
    void unVerdictEfficaceClotLeDossierEtConsigneSonEvaluation() {
        UUID auteur = UUID.randomUUID();
        connecte(auteur, "A. Diallo");
        CapaCase c = aJuger("Moins de 2 % de rebuts sur 3 mois");
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE, "1,1 %"));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
        assertThat(c.getClosedAt()).isNotNull();
        assertThat(c.getEffectivenessVerified()).isTrue();
        com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview evaluation = captureEvaluation();
        assertThat(evaluation.verdict()).isEqualTo(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE);
        assertThat(evaluation.observed()).isEqualTo("1,1 %");
        assertThat(evaluation.baseline()).isEqualTo("7,4 %");
        assertThat(evaluation.target()).isEqualTo("< 2 %");
        assertThat(evaluation.comment()).isEqualTo("mesure du mois");
        assertThat(evaluation.capaCaseId()).isEqualTo(c.getId());
        assertThat(evaluation.tenantId()).isEqualTo(TENANT);
        // L'auteur vient du JETON, jamais du corps de la requete (OWASP A01).
        assertThat(evaluation.reviewerId()).isEqualTo(auteur);
        assertThat(evaluation.reviewerName()).isEqualTo("A. Diallo");
        verify(journal).record(c, CapaTransition.CLOSED);
    }

    @Test
    void leCritereEstRecopieDansLEvaluationEtNonReference() {
        // Recopie, pour que modifier le critere du dossier plus tard ne reecrive
        // pas le jugement rendu contre l'ancien. C'est la difference entre un
        // historique et une photographie de l'etat courant.
        connecte(UUID.randomUUID(), "A. Diallo");
        CapaCase c = aJuger("Moins de 2 % de rebuts sur 3 mois");
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.PARTIAL, "3,1 %"));
        c.setEffectivenessCriterion("un critere tout autre");

        assertThat(captureEvaluation().criterion()).isEqualTo("Moins de 2 % de rebuts sur 3 mois");
    }

    @Test
    void unVerdictPartielRepartLeDossierAuTravailSansLeClore() {
        connecte(UUID.randomUUID(), "A. Diallo");
        CapaCase c = aJuger("Moins de 2 % de rebuts sur 3 mois");
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.PARTIAL, "3,1 %"));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
        assertThat(c.getResolvedAt()).isNull();
        assertThat(c.getClosedAt()).isNull();
        // L'ancien booleen ne sait dire que oui ou non : PARTIEL y vaut non.
        assertThat(c.getEffectivenessVerified()).isFalse();
        assertThat(c.getEffectivenessVerifiedAt()).isNotNull();
        verify(journal).record(c, CapaTransition.EFFECTIVENESS_PARTIAL);
    }

    @Test
    void unVerdictQuiNeClotPasNExigePasQueToutSoitFait() {
        // Constater l'insuffisance n'est pas clore : exiger que toutes les actions
        // soient faites empecherait de consigner que les actions en cours ne
        // produisent pas l'effet attendu.
        connecte(UUID.randomUUID(), "A. Diallo");
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setEffectivenessCriterion("Moins de 2 % de rebuts");
        action(c, CapaActionStatus.PENDING);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.PARTIAL, "5,0 %"));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void unVerdictPartielNEstPasRefusePourEndiguementSeul() {
        connecte(UUID.randomUUID(), "A. Diallo");
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setEffectivenessCriterion("Moins de 2 % de rebuts");
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.PARTIAL, "5,0 %"));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void lesDeuxFormesEnsembleSontRefuseesPlutotQuArbitrees() {
        // Deux jugements dans une meme requete disent que l'appelant ne sait pas
        // lequel il envoie ; en choisir un pour lui enterrerait son defaut.
        CapaCase c = aJuger("Moins de 2 % de rebuts");

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(Boolean.TRUE, com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE,
                        null, null, "1,1 %", null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("not both");

        verifyNoInteractions(reviews);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void aucuneDesDeuxFormesEstRefuseeEnNommantLesTroisEtats() {
        CapaCase c = aJuger("Moins de 2 % de rebuts");

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(null, null, null, null, "1,1 %", null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("EFFECTIVE")
                .hasMessageContaining("PARTIAL")
                .hasMessageContaining("INEFFECTIVE");

        verifyNoInteractions(reviews);
    }

    @Test
    void unVerdictSansReleveEstRefuse() {
        // Un verdict sans mesure n'est pas un jugement, c'est une opinion.
        CapaCase c = aJuger("Moins de 2 % de rebuts");

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(null, com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE, null, null, "   ", null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("observed");

        verifyNoInteractions(reviews);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void jugerSansCritereEstRefuseEtLeMessageDitOuLePoser() {
        // Juger sans critere annonce revient a choisir le critere en regardant le
        // resultat. Le refus nomme le geste a faire plutot que le champ vide.
        CapaCase c = aJuger(null);

        assertThatThrownBy(() -> service.verifyEffectiveness(c.getId(),
                verdict(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE, "1,1 %")))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("criterion")
                .hasMessageContaining("before evaluating");

        verifyNoInteractions(reviews);
        verify(caseRepo, never()).save(any());
    }

    @Test
    void lAncienneApiContinueDeFonctionnerSurUnDossierSansCritere() {
        // Exiger un critere de l'ancien booleen casserait les integrations
        // existantes et rendrait inevaluables tous les dossiers ouverts avant
        // cette fonctionnalite. On inscrit une mention explicite.
        CapaCase c = aJuger(null);
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(true));

        assertThat(c.getStatus()).isEqualTo(CapaStatus.CLOSED);
        com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview evaluation = captureEvaluation();
        assertThat(evaluation.criterion()).isEqualTo(CapaService.LEGACY_CRITERION);
        assertThat(evaluation.observed()).isEqualTo(CapaService.LEGACY_OBSERVED);
        assertThat(evaluation.verdict()).isEqualTo(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.EFFECTIVE);
    }

    @Test
    void lAncienneApiNeProduitJamaisLeTroisiemeEtat() {
        // Traduire false en PARTIEL inventerait une nuance que l'appelant n'a pas
        // exprimee, et inscrirait au registre un jugement que personne n'a rendu.
        CapaCase c = aJuger("Moins de 2 % de rebuts");
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        assertThat(captureEvaluation().verdict()).isEqualTo(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.INEFFECTIVE);
        verify(journal).record(c, CapaTransition.EFFECTIVENESS_REJECTED);
    }

    @Test
    void lAncienneApiHonoreLeCritereDuDossierQuandIlYEnA() {
        CapaCase c = aJuger("Moins de 2 % de rebuts sur 3 mois");
        when(caseRepo.save(c)).thenReturn(c);

        service.verifyEffectiveness(c.getId(), new CapaDto.EffectivenessRequest(false));

        assertThat(captureEvaluation().criterion()).isEqualTo("Moins de 2 % de rebuts sur 3 mois");
    }

    @Test
    void laFicheDetailleeRendLeCritereEtLeDernierVerdict() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setEffectivenessCriterion("Moins de 2 % de rebuts");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(reviews.findLastByCase(TENANT, c.getId())).thenReturn(Optional.of(
                com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview.record(UUID.randomUUID(), TENANT, c.getId(), com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.PARTIAL,
                        "Moins de 2 % de rebuts", null, null, "3,1 %", null, null, null,
                        Instant.now())));

        CapaDto.CaseResponse r = service.findById(c.getId());

        assertThat(r.effectivenessCriterion()).isEqualTo("Moins de 2 % de rebuts");
        assertThat(r.effectivenessVerdict()).isEqualTo(com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.PARTIAL);
    }

    @Test
    void laNoteDEfficaciteSEnregistreEtSeRelitSurLaFiche() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCase(c.getId(), new CapaDto.UpdateCaseRequest(null, null, null, null, null, null,
                null, null, null, null, null, "Zero retour client sur deux trimestres"));

        assertThat(c.getEffectivenessComment()).isEqualTo("Zero retour client sur deux trimestres");
        assertThat(service.findById(c.getId()).effectivenessComment())
                .isEqualTo("Zero retour client sur deux trimestres");
    }

    @Test
    void laNoteDEfficaciteNEstNiLeCritereNiLeVerdict() {
        // Le critere dit ce qu'on mesurera, le verdict est rendu et signe ; la
        // note est libre. Ecrire la note ne doit toucher ni l'un ni l'autre.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setEffectivenessCriterion("Moins de 2 % de rebuts");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.updateCase(c.getId(), new CapaDto.UpdateCaseRequest(null, null, null, null, null, null,
                null, null, null, null, null, "Suivi sur trois mois, aucun ecart"));

        assertThat(c.getEffectivenessCriterion()).isEqualTo("Moins de 2 % de rebuts");
        assertThat(c.getEffectivenessComment()).isEqualTo("Suivi sur trois mois, aucun ecart");
    }

    @Test
    void laNoteDEfficaciteNeSEcritPlusSurUnDossierClos() {
        // Un dossier clos ne se recrit pas : sa note non plus.
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.updateCase(c.getId(),
                new CapaDto.UpdateCaseRequest(null, null, null, null, null, null,
                        null, null, null, null, null, "tentative apres cloture")))
                .isInstanceOf(CapaStateException.class);

        assertThat(c.getEffectivenessComment()).isNull();
    }

    @Test
    void laListePagineeNeVaPasChercherLeDernierVerdictParLigne() {
        // Une requete par ligne pour une information que la liste n'affiche pas.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByTenantId(any(), any())).thenReturn(new PageImpl<>(List.of(c)));

        service.findAll(null, PageRequest.of(0, 20));

        verify(reviews, never()).findLastByCase(any(), any());
    }

    @Test
    void leCritereSePoseALOuvertureEtSeModifieTantQuAucunJugementNEstRendu() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);
        when(reviews.findByCase(TENANT, c.getId())).thenReturn(List.of());

        service.updateCase(c.getId(), new CapaDto.UpdateCaseRequest(null, null, null, null, null,
                null, null, null, null, null, "  Moins de 2 % de rebuts sur 3 mois  ", null));

        assertThat(c.getEffectivenessCriterion()).isEqualTo("Moins de 2 % de rebuts sur 3 mois");
    }

    @Test
    void leCritereNeSeModifiePlusDesQuUnJugementAEteRendu() {
        // Les verdicts passes ont ete rendus CONTRE lui : le changer reecrirait
        // apres coup la question a laquelle on a repondu.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setEffectivenessCriterion("Moins de 2 % de rebuts");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(reviews.findByCase(TENANT, c.getId())).thenReturn(List.of(
                com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview.record(UUID.randomUUID(), TENANT, c.getId(), com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict.INEFFECTIVE,
                        "Moins de 2 % de rebuts", null, null, "9 %", null, null, null,
                        Instant.now())));

        assertThatThrownBy(() -> service.updateCase(c.getId(),
                new CapaDto.UpdateCaseRequest(null, null, null, null, null, null,
                        null, null, null, null, "un critere tout autre", null)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("already been evaluated");

        assertThat(c.getEffectivenessCriterion()).isEqualTo("Moins de 2 % de rebuts");
    }

    @Test
    void renvoyerLeMemeCritereNEstPasUneModification() {
        // L'ecran poste souvent la fiche entiere : un PATCH qui renvoie le champ
        // inchange ne doit pas etre refuse sur un dossier deja juge.
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setEffectivenessCriterion("Moins de 2 % de rebuts");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(c)).thenReturn(c);

        service.updateCase(c.getId(), new CapaDto.UpdateCaseRequest(null, null, null, null, null,
                null, null, null, null, null, "Moins de 2 % de rebuts", null));

        assertThat(c.getEffectivenessCriterion()).isEqualTo("Moins de 2 % de rebuts");
        verify(reviews, never()).findByCase(any(), any());
    }

    // --- les dossiers qui retiennent un ecart ---

    /**
     * TOUS les dossiers de l ecart, pas seulement celui de l escalade.
     *
     * <p>Un dossier ouvert depuis le registre des CAPA en citant la reference
     * de l ecart ne laisse aucune trace dans {@code capaCaseId}. C est par la
     * reference qu on le retrouve, et il retient la cloture de l ecart au meme
     * titre que celui de l escalade.
     */
    @Test
    void unsettledCasesFor_reunitLEscaladeEtLesDossiersLiesParLaReference() {
        CapaCase escalade = capa(TENANT, CapaStatus.IN_PROGRESS);
        escalade.setTitle("Remplacer le roulement");
        CapaCase parReference = capa(TENANT, CapaStatus.RESOLVED);
        parReference.setTitle("Auditer le fournisseur");

        when(caseRepo.findByIdAndTenantId(escalade.getId(), TENANT))
                .thenReturn(Optional.of(escalade));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                TENANT, CapaSourceType.NON_CONFORMITY, "NC-2026-0001"))
                .thenReturn(List.of(parReference));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                TENANT, CapaSourceType.EXTERNAL_NON_CONFORMITY, "NC-2026-0001"))
                .thenReturn(List.of());

        assertThat(service.unsettledCasesFor(escalade.getId(), "NC-2026-0001"))
                // RESOLVED en fait partie : il reste a juger de l efficacite,
                // et c est ce jugement qui clot (ADR 0108).
                .containsExactly("Remplacer le roulement", "Auditer le fournisseur");
    }

    /**
     * Le dossier de l escalade est souvent AUSSI lie par sa reference : il ne
     * doit etre compte qu une fois, sans quoi le refus annonce deux travaux
     * la ou il n y en a qu un.
     */
    @Test
    void unsettledCasesFor_neCompteQuUneFoisLeMemeDossier() {
        CapaCase unique = capa(TENANT, CapaStatus.OPEN);
        unique.setTitle("Revoir le mode operatoire");

        when(caseRepo.findByIdAndTenantId(unique.getId(), TENANT))
                .thenReturn(Optional.of(unique));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("NC-2026-0001")))
                .thenReturn(List.of(unique));

        assertThat(service.unsettledCasesFor(unique.getId(), "NC-2026-0001"))
                .containsExactly("Revoir le mode operatoire");
    }

    /**
     * LE DOSSIER OUVERT DEPUIS LE REGISTRE CAPA EST RETROUVE.
     *
     * <p>Trois valeurs pour deux choses : {@code EXTERNAL_NON_CONFORMITY} et
     * {@code COMPLAINT} nomment le meme registre -- celui des ecarts signales
     * du dehors -- et seule la seconde est proposee a la creation manuelle. Un
     * dossier ouvert depuis le registre CAPA citait donc la bonne reference
     * sous une source que la fiche de l'ecart ne regardait pas : il restait
     * invisible au D3, au D5, au D7 et dans le rapport 8D, alors que ses
     * actions retenaient la cloture.
     */
    @Test
    void casesOf_retrouve_un_dossier_ouvert_sous_la_source_reclamation() {
        CapaCase manuel = capa(TENANT, CapaStatus.OPEN);
        manuel.setTitle("Dossier ouvert depuis le registre");
        manuel.setSourceType(CapaSourceType.COMPLAINT);
        manuel.setSourceRef("Ext-NC-2026-0011");

        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                TENANT, CapaSourceType.COMPLAINT, "Ext-NC-2026-0011"))
                .thenReturn(List.of(manuel));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT),
                ArgumentMatchers.argThat(t -> t != CapaSourceType.COMPLAINT),
                eq("Ext-NC-2026-0011")))
                .thenReturn(List.of());

        assertThat(service.casesOf(null, "Ext-NC-2026-0011"))
                .extracting(CapaDto.CaseRef::title)
                .containsExactly("Dossier ouvert depuis le registre");
    }

    /**
     * La REFERENCE reste la condition : une reclamation du registre des
     * reclamations ne porte pas la reference d'un ecart, et n'entre donc pas
     * par cette porte, meme avec la meme source.
     */
    @Test
    void casesOf_ne_ramasse_pas_un_dossier_d_une_autre_reference() {
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("Ext-NC-2026-0011")))
                .thenReturn(List.of());

        assertThat(service.casesOf(null, "Ext-NC-2026-0011")).isEmpty();
    }

    /**
     * CE QUI RETIENT LE DOSSIER, ACTION PAR ACTION.
     *
     * <p>Le motif de blocage d un ecart nommait le dossier et s arretait la.
     * Un dossier porte dix actions ; savoir qu il n est pas clos ne dit pas
     * laquelle on attend, et la personne devait les ouvrir une a une pour
     * trouver celle qui resiste.
     *
     * <p>Les deux memes regles que la cloture : pas DONE, ou faite sans que son
     * effet soit verifie (ADR 0132). Une action faite ET verifiee ne parait
     * pas -- elle ne retient plus rien.
     */
    @Test
    void casesOf_nommeLesActionsQuiRetiennentLeDossier() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        c.setSourceRef("NC-2026-0001");
        CapaAction aFaire = action(c, CapaActionStatus.IN_PROGRESS);
        aFaire.setTitle("Reprendre les 12 vannes");
        CapaAction aVerifier = actionPromise(c);
        aVerifier.setTitle("Imprimer la plaque depuis l ERP");
        CapaAction reglee = actionPromise(c);
        reglee.setTitle("Mettre a jour l instruction");

        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("NC-2026-0001")))
                .thenReturn(List.of(c));
        when(reviews.findLastPerActionOfCase(TENANT, c.getId()))
                .thenReturn(Map.of(reglee.getId(),
                        jugement(c, reglee, EffectivenessVerdict.EFFECTIVE)));

        assertThat(service.casesOf(null, "NC-2026-0001"))
                .singleElement()
                .extracting(CapaDto.CaseRef::blockingActions, list(CapaDto.BlockingAction.class))
                .extracting(CapaDto.BlockingAction::title, CapaDto.BlockingAction::reason)
                .containsExactlyInAnyOrder(
                        tuple("Reprendre les 12 vannes", CapaDto.BlockingReason.TO_DO),
                        tuple("Imprimer la plaque depuis l ERP",
                                CapaDto.BlockingReason.TO_VERIFY));
    }

    /**
     * Un dossier REGLE ne retient plus rien, et on ne va pas lire ses
     * jugements d efficacite pour l apprendre.
     */
    @Test
    void casesOf_neNommeAucuneActionSurUnDossierClos() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        c.setSourceRef("NC-2026-0001");
        action(c, CapaActionStatus.PENDING);

        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("NC-2026-0001")))
                .thenReturn(List.of(c));

        assertThat(service.casesOf(null, "NC-2026-0001"))
                .singleElement()
                .extracting(CapaDto.CaseRef::blockingActions, list(CapaDto.BlockingAction.class))
                .isEmpty();
        verify(reviews, never()).findLastPerActionOfCase(any(), any());
    }

    /**
     * casesOf NOMME les dossiers : la portee s y applique donc (ADR 0081).
     *
     * <p>Sans la permission de voir tous les dossiers, on ne voit que ceux qui
     * vous concernent. Rendre ici le titre d un dossier hors de portee le
     * divulguerait par un chemin detourne -- la fiche d un ecart -- alors que
     * sa propre fiche le refuse.
     */
    @Test
    void casesOf_neNommePasUnDossierHorsDePortee() {
        UUID moi = UUID.randomUUID();
        CapaCase lemien = capa(TENANT, CapaStatus.OPEN);
        lemien.setTitle("Le mien");
        lemien.setOwnerId(moi);
        CapaCase dUnAutre = capa(TENANT, CapaStatus.OPEN);
        dUnAutre.setTitle("Celui d un autre");
        dUnAutre.setOwnerId(UUID.randomUUID());

        when(scope.restrictTo(
                com.openlab.qualitos.quality.authz.domain.Permission.CAPA_VIEW_ALL))
                .thenReturn(Optional.of(moi));
        when(caseRepo.findByIdAndTenantId(lemien.getId(), TENANT))
                .thenReturn(Optional.of(lemien));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("NC-2026-0001")))
                .thenReturn(List.of(lemien, dUnAutre));

        assertThat(service.casesOf(lemien.getId(), "NC-2026-0001"))
                .extracting(CapaDto.CaseRef::title)
                .containsExactly("Le mien");
    }

    /**
     * Les NOMS que porte un refus suivent la meme portee que casesOf.
     *
     * <p>Le refus de resolution enumerait les titres rendus par la question
     * hors portee : un lecteur restreint a ses propres dossiers lisait, dans un
     * message d erreur, le titre d un dossier que sa propre fiche lui refuse.
     * Le decompte, lui, reste complet -- il est ailleurs.
     */
    @Test
    void namesInScope_neNommentPasUnDossierHorsDePortee() {
        UUID moi = UUID.randomUUID();
        CapaCase lemien = capa(TENANT, CapaStatus.OPEN);
        lemien.setTitle("Le mien");
        lemien.setOwnerId(moi);
        CapaCase dUnAutre = capa(TENANT, CapaStatus.IN_PROGRESS);
        dUnAutre.setTitle("Celui d un autre");
        dUnAutre.setOwnerId(UUID.randomUUID());

        when(scope.restrictTo(
                com.openlab.qualitos.quality.authz.domain.Permission.CAPA_VIEW_ALL))
                .thenReturn(Optional.of(moi));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("NC-2026-0001")))
                .thenReturn(List.of(lemien, dUnAutre));

        assertThat(service.unexecutedCaseNamesInScopeFor(null, "NC-2026-0001"))
                .containsExactly("Le mien");
        assertThat(service.unsettledCaseNamesInScopeFor(null, "NC-2026-0001"))
                .containsExactly("Le mien");
        // Et le decompte, lui, les voit tous les deux.
        assertThat(service.unexecutedCasesFor(null, "NC-2026-0001")).hasSize(2);
    }

    /**
     * L invariant, lui, COMPTE TOUT. Un refus de cloture qui dependrait de qui
     * regarde ne serait plus un invariant : l ecart se cloturerait pour l un et
     * pas pour l autre. C est precisement pourquoi un motif de blocage porte un
     * DECOMPTE et jamais un titre.
     */
    @Test
    void unsettledCasesFor_compteAussiLesDossiersHorsDePortee() {
        CapaCase dUnAutre = capa(TENANT, CapaStatus.IN_PROGRESS);
        dUnAutre.setTitle("Celui d un autre");
        dUnAutre.setOwnerId(UUID.randomUUID());

        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("NC-2026-0001")))
                .thenReturn(List.of(dUnAutre));

        assertThat(service.unsettledCasesFor(null, "NC-2026-0001")).hasSize(1);
    }

    /** Clos ou ecarte : le dossier ne retient plus rien. */
    @Test
    void unsettledCasesFor_ignoreLesDossiersClosOuEcartes() {
        CapaCase clos = capa(TENANT, CapaStatus.CLOSED);
        CapaCase ecarte = capa(TENANT, CapaStatus.REJECTED);

        when(caseRepo.findByIdAndTenantId(clos.getId(), TENANT)).thenReturn(Optional.of(clos));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("NC-2026-0001")))
                .thenReturn(List.of(ecarte));

        assertThat(service.unsettledCasesFor(clos.getId(), "NC-2026-0001")).isEmpty();
    }

    /**
     * Un ecart sans escalade et sans reference lisible ne retient rien : la
     * question ne doit pas partir en base pour rien, ni lever quoi que ce soit.
     */
    @Test
    void unsettledCasesFor_sansEscaladeNiReference_neRetientRien() {
        assertThat(service.unsettledCasesFor(null, "  ")).isEmpty();
        verify(caseRepo, never()).findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                any(), any(), any());
    }

    // --- le dossier ouvert depuis la liste rejoint son ecart (V170) ---

    /**
     * Un dossier qui cite la reference d un ecart EXTERNE est marque externe,
     * quelle que soit la source demandee.
     *
     * <p>La liste CAPA ne propose qu une source d ecart, « Non-conformite
     * interne » : un dossier ouvert depuis la pour une reclamation client
     * s affichait donc « interne » sur l ecran qui sert a repondre a ce client.
     * L origine est deja ecrite sur l ecart ; on la lit au lieu de la demander.
     */
    @Test
    void openCase_citantUnEcartExterne_prendLaSourceDeLEcart() {
        NonConformity ecart = ecart("Ext-NC-2026-0004", NcOrigin.EXTERNAL);
        stubEcart(ecart);
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.CaseResponse r = service.openCase(
                openReq(CapaType.CORRECTIVE, LocalDate.now().plusDays(10), "Pilote"),
                CapaSourceType.NON_CONFORMITY, " Ext-NC-2026-0004 ");

        assertThat(r.sourceType()).isEqualTo(CapaSourceType.EXTERNAL_NON_CONFORMITY);
    }

    /**
     * Une source « reclamation client » qui cite un ecart le REJOINT, comme
     * les deux sources d ecart.
     *
     * <p>Une plainte client se constate comme un ecart externe, et c est cet
     * ecart que le dossier cite. La source ne comptait pas comme un ecart : le
     * lien n etait pas pose, le dossier n apparaissait sur aucune fiche, ne
     * retenait aucune cloture et manquait au 8D.
     */
    @Test
    void openCase_sourceReclamation_citantUnEcart_leRejoint() {
        NonConformity ecart = ecart("Ext-NC-2026-0040", NcOrigin.EXTERNAL);
        stubEcart(ecart);
        UUID dossierId = UUID.randomUUID();
        when(caseRepo.save(any())).thenAnswer(inv -> {
            CapaCase c = inv.getArgument(0);
            c.setId(dossierId);
            return c;
        });

        CapaDto.CaseResponse r = service.openCase(
                openReq(CapaType.CORRECTIVE, LocalDate.now().plusDays(10), "Pilote"),
                CapaSourceType.COMPLAINT, "Ext-NC-2026-0040");

        assertThat(r.sourceType()).isEqualTo(CapaSourceType.EXTERNAL_NON_CONFORMITY);
        assertThat(ecart.getCapaCaseId()).isEqualTo(dossierId);
        verify(ncRepo).save(ecart);
    }

    /** Et l inverse : citer un ecart interne ramene la source a « interne ». */
    @Test
    void openCase_citantUnEcartInterne_prendLaSourceDeLEcart() {
        NonConformity ecart = ecart("Int-NC-2026-0007", NcOrigin.INTERNAL);
        stubEcart(ecart);
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.CaseResponse r = service.openCase(
                openReq(CapaType.CORRECTIVE, LocalDate.now().plusDays(10), "Pilote"),
                CapaSourceType.EXTERNAL_NON_CONFORMITY, "Int-NC-2026-0007");

        assertThat(r.sourceType()).isEqualTo(CapaSourceType.NON_CONFORMITY);
    }

    /**
     * Le lien est POSE sur l ecart : sans lui, la fiche de l ecart n affichait
     * pas le tableau d actions du dossier, le rapport 8D ne le voyait pas, et
     * la cloture de l ecart ne l attendait pas.
     */
    @Test
    void openCase_citantUnEcartSansDossier_poseLeLienSurLEcart() {
        NonConformity ecart = ecart("Int-NC-2026-0007", NcOrigin.INTERNAL);
        stubEcart(ecart);
        UUID dossierId = UUID.randomUUID();
        when(caseRepo.save(any())).thenAnswer(inv -> {
            CapaCase c = inv.getArgument(0);
            c.setId(dossierId);
            return c;
        });

        service.openCase(openReq(CapaType.CORRECTIVE, LocalDate.now().plusDays(10), "Pilote"),
                CapaSourceType.NON_CONFORMITY, "Int-NC-2026-0007");

        assertThat(ecart.getCapaCaseId()).isEqualTo(dossierId);
        verify(ncRepo).save(ecart);
    }

    /**
     * Un ecart qui a DEJA son dossier garde le sien : un ecart a un dossier
     * (ADR 0088), et remplacer le lien ferait disparaitre de la fiche le
     * tableau d actions du premier. Le second reste rattache par la reference,
     * donc la cloture l attend quand meme.
     */
    @Test
    void openCase_citantUnEcartDejaRattache_neRemplacePasLeLien() {
        NonConformity ecart = ecart("Int-NC-2026-0007", NcOrigin.INTERNAL);
        UUID premier = UUID.randomUUID();
        ecart.setCapaCaseId(premier);
        stubEcart(ecart);
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.openCase(openReq(CapaType.CORRECTIVE, LocalDate.now().plusDays(10), "Pilote"),
                CapaSourceType.NON_CONFORMITY, "Int-NC-2026-0007");

        assertThat(ecart.getCapaCaseId()).isEqualTo(premier);
        verify(ncRepo, never()).save(any());
    }

    /**
     * Une source qui ne designe pas le registre des ecarts ne declenche aucune
     * lecture : un dossier d audit qui cite « AUD-2026-Q2 » n a pas a chercher
     * un ecart de ce nom.
     */
    @Test
    void openCase_sourceQuiNEstPasUnEcart_neChercheAucunEcart() {
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.openCase(openReq(CapaType.CORRECTIVE, LocalDate.now().plusDays(10), "Pilote"),
                CapaSourceType.AUDIT, "AUD-2026-Q2");

        verify(ncRepo, never()).findByTenantIdAndReference(any(), any());
    }

    /** Une reference qui ne designe aucun ecart laisse le dossier tel quel. */
    @Test
    void openCase_referenceInconnue_laisseLaSourceDemandee() {
        when(ncRepo.findByTenantIdAndReference(TENANT, "Int-NC-9999")).thenReturn(Optional.empty());
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.CaseResponse r = service.openCase(
                openReq(CapaType.CORRECTIVE, LocalDate.now().plusDays(10), "Pilote"),
                CapaSourceType.NON_CONFORMITY, "Int-NC-9999");

        assertThat(r.sourceType()).isEqualTo(CapaSourceType.NON_CONFORMITY);
        verify(ncRepo, never()).save(any());
    }

    // --- les dossiers dont les actions restent a mener ---

    /**
     * Resoudre un ecart demande que les actions soient EXECUTEES ; juger de leur
     * efficacite vient apres, et c est la cloture qui l attend (ADR 0108). Un
     * dossier resolu ne retient donc pas la resolution, un dossier en cours si.
     */
    @Test
    void unexecutedCasesFor_neRetientQueOpenEtInProgress() {
        CapaCase enCours = capa(TENANT, CapaStatus.IN_PROGRESS);
        enCours.setTitle("Remplacer le roulement");
        CapaCase resolu = capa(TENANT, CapaStatus.RESOLVED);
        resolu.setTitle("Auditer le fournisseur");
        CapaCase ouvert = capa(TENANT, CapaStatus.OPEN);
        ouvert.setTitle("Revoir le mode operatoire");

        when(caseRepo.findByIdAndTenantId(enCours.getId(), TENANT))
                .thenReturn(Optional.of(enCours));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("Int-NC-2026-0007")))
                .thenReturn(List.of(resolu, ouvert));

        assertThat(service.unexecutedCasesFor(enCours.getId(), "Int-NC-2026-0007"))
                .containsExactly("Remplacer le roulement", "Revoir le mode operatoire");
    }

    /** Clos ou ecarte : plus rien a executer. */
    @Test
    void unexecutedCasesFor_ignoreLesDossiersClosOuEcartes() {
        CapaCase clos = capa(TENANT, CapaStatus.CLOSED);
        CapaCase ecarte = capa(TENANT, CapaStatus.REJECTED);

        when(caseRepo.findByIdAndTenantId(clos.getId(), TENANT)).thenReturn(Optional.of(clos));
        when(caseRepo.findByTenantIdAndSourceTypeAndSourceRefOrderByCreatedAtAsc(
                eq(TENANT), any(), eq("Int-NC-2026-0007")))
                .thenReturn(List.of(ecarte));

        assertThat(service.unexecutedCasesFor(clos.getId(), "Int-NC-2026-0007")).isEmpty();
    }

    /** L ecart sous verrou d ecriture, et sa reference lue sans ses espaces. */
    private void stubEcart(NonConformity ecart) {
        when(ncRepo.findByTenantIdAndReference(TENANT, ecart.getReference()))
                .thenReturn(Optional.of(ecart));
        when(ncRepo.findForUpdateByIdAndTenantId(ecart.getId(), TENANT))
                .thenReturn(Optional.of(ecart));
    }

    private static NonConformity ecart(String reference, NcOrigin origine) {
        NonConformity nc = new NonConformity();
        nc.setId(UUID.randomUUID());
        nc.setTenantId(TENANT);
        nc.setReference(reference);
        nc.setOrigin(origine);
        return nc;
    }

    // --- l'evaluation d'efficacite DE CHAQUE ACTION (ADR 0131) ------------------
    //
    // Le critere et la decision de verification etaient ceux du DOSSIER, poses
    // une seule fois a l'ouverture : la deuxieme action et les suivantes
    // s'ecrivaient donc sans qu'on sache jamais si leur effet serait verifie.
    // Les deux coexistent desormais -- le dossier garde les siens, qui
    // gouvernent sa cloture, chaque action porte les siens.

    @Test
    void addAction_porteSonProprePritereEtSaPropreVerification() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setEffectivenessCriterion("Aucun rebut pendant 3 mois");
        action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID verificateur = UUID.randomUUID();

        CapaDto.ActionResponse r = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Former les operateurs de nuit", null, null, CapaActionType.PREVENTIVE,
                null, null, null, null,
                "  Zero recidive sur 6 mois  ", true, verificateur, "  A. Diallo  ",
                "  Relire les releves de la ligne 3  "));

        assertThat(r.effectivenessCriterion()).isEqualTo("Zero recidive sur 6 mois");
        assertThat(r.verificationRequired()).isTrue();
        assertThat(r.verificationAssigneeId()).isEqualTo(verificateur);
        assertThat(r.verificationAssigneeName()).isEqualTo("A. Diallo");
        assertThat(r.verificationInstructions()).isEqualTo("Relire les releves de la ligne 3");
        // Le dossier garde le sien, intact : l'action ne decide pas pour lui.
        assertThat(c.getEffectivenessCriterion()).isEqualTo("Aucun rebut pendant 3 mois");
    }

    /**
     * Un endiguement arrete l'effet, il ne pretend pas supprimer la cause
     * (ISO 9001 10.2) : il n'y a rien a mesurer sur lui. Accepter un critere
     * puis l'ignorer laisserait croire qu'une mesure a ete promise.
     */
    @Test
    void addAction_refuseUnCritereOuUneVerificationSurUnEndiguement() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Bloquer le lot", null, null, CapaActionType.CONTAINMENT, null, null, null, null,
                "Aucun rebut", null, null, null, null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("containment");

        assertThatThrownBy(() -> service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Bloquer le lot", null, null, CapaActionType.CONTAINMENT, null, null, null, null,
                null, true, UUID.randomUUID(), null, null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("containment");
    }

    @Test
    void addAction_refuseUneVerificationExigeeSansVerificateur() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Recalibrer la presse", null, null, CapaActionType.CORRECTIVE, null, null, null, null,
                null, true, null, null, null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("assigned");
    }

    /**
     * Designer un verificateur sans repondre a la question n est pas une
     * decision : la verification n aurait lieu que si quelqu un y pense. Meme
     * garde que sur le dossier.
     */
    @Test
    void addAction_refuseUnVerificateurSansReponseALaQuestion() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Recalibrer la presse", null, null, CapaActionType.CORRECTIVE, null, null, null, null,
                null, null, UUID.randomUUID(), null, null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("verificationRequired=true");
    }

    /**
     * Un « non » EFFACE le verificateur et les consignes, au lieu d etre
     * refuse : c est exactement ce que fait la meme decision sur le dossier, et
     * deux regles divergentes sur la meme question se liraient comme un bogue.
     */
    @Test
    void addAction_uneVerificationRefusee_effaceLeVerificateur() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "Recalibrer la presse", null, null, CapaActionType.CORRECTIVE, null, null, null, null,
                null, false, UUID.randomUUID(), "A. Diallo", "Relire les releves"));

        assertThat(r.verificationRequired()).isFalse();
        assertThat(r.verificationAssigneeId()).isNull();
        assertThat(r.verificationAssigneeName()).isNull();
        assertThat(r.verificationInstructions()).isNull();
    }

    /**
     * Le critere d'une action se VERROUILLE des qu'il est pose : le reecrire
     * plus tard, c'est deplacer la cible entre deux mesures (ADR 0108). Le meme
     * texte renvoye a l'identique passe, lui : un PATCH qui n'y touche pas n'a
     * pas a etre refuse.
     */
    @Test
    void updateAction_refuseDeReecrireLeCritereDUneAction_maisAccepteLeMemeTexte() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = action(c, CapaActionStatus.PENDING);
        a.setEffectivenessCriterion("Zero recidive sur 6 mois");
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.updateAction(c.getId(), a.getId(), new CapaDto.ActionRequest(
                null, null, null, null, null, null, null, null,
                "Un seul rebut tolere", null, null, null, null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("cannot be rewritten");

        CapaDto.ActionResponse r = service.updateAction(c.getId(), a.getId(),
                new CapaDto.ActionRequest(null, null, null, null, null, null, null, null,
                        "  Zero recidive sur 6 mois  ", null, null, null, null));
        assertThat(r.effectivenessCriterion()).isEqualTo("Zero recidive sur 6 mois");
    }

    /**
     * La premiere action recopie ce qui vient d'etre pose sur le dossier : la
     * question n'a ete posee qu'une fois, en ecrivant cette action-la, et elle
     * serait sinon la seule du dossier a ne rien dire de son effet.
     */
    @Test
    void openCase_recopieLEfficaciteDuDossierSurSaPremiereAction() {
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID pilote = UUID.randomUUID();
        UUID verificateur = UUID.randomUUID();

        service.openCase(new CapaDto.OpenCaseRequest(
                "Recalibrer la sonde", null, CapaType.CORRECTIVE, CapaActionType.CORRECTIVE,
                CapaCriticity.HIGH, CapaSourceType.OTHER, null, pilote, "A. Diallo",
                LocalDate.now().plusDays(10), true, verificateur, "B. Nour", "Relire les releves",
                "Aucun rebut pendant 3 mois"), CapaSourceType.OTHER, null);

        ArgumentCaptor<CapaCase> saved = ArgumentCaptor.forClass(CapaCase.class);
        verify(caseRepo).save(saved.capture());
        CapaAction premiere = saved.getValue().getActions().iterator().next();
        assertThat(premiere.getEffectivenessCriterion()).isEqualTo("Aucun rebut pendant 3 mois");
        assertThat(premiere.getVerificationRequired()).isTrue();
        assertThat(premiere.getVerificationAssigneeId()).isEqualTo(verificateur);
        assertThat(premiere.getVerificationAssigneeName()).isEqualTo("B. Nour");
        assertThat(premiere.getVerificationInstructions()).isEqualTo("Relire les releves");
    }

    /** Rien sous un endiguement : il ne promet aucun effet. */
    @Test
    void openCase_neRecopieRienSurUnPremierEndiguement() {
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID pilote = UUID.randomUUID();
        UUID verificateur = UUID.randomUUID();

        service.openCase(new CapaDto.OpenCaseRequest(
                "Bloquer le lot", null, CapaType.CORRECTIVE, CapaActionType.CONTAINMENT,
                CapaCriticity.HIGH, CapaSourceType.OTHER, null, pilote, "A. Diallo",
                LocalDate.now().plusDays(10), true, verificateur, "B. Nour", null,
                "Aucun rebut pendant 3 mois"), CapaSourceType.OTHER, null);

        ArgumentCaptor<CapaCase> saved = ArgumentCaptor.forClass(CapaCase.class);
        verify(caseRepo).save(saved.capture());
        CapaAction premiere = saved.getValue().getActions().iterator().next();
        assertThat(premiere.getActionType()).isEqualTo(CapaActionType.CONTAINMENT);
        assertThat(premiere.getEffectivenessCriterion()).isNull();
        assertThat(premiere.getVerificationRequired()).isNull();
        // Le DOSSIER, lui, garde la decision prise a son ouverture.
        assertThat(saved.getValue().getVerificationRequired()).isTrue();
        assertThat(saved.getValue().getEffectivenessCriterion()).isEqualTo("Aucun rebut pendant 3 mois");
    }

    // --- TOUTES LES ACTIONS CLOSES AVANT LE DOSSIER (ADR 0132) -----------------
    //
    // Une action « faite » dit que le geste a eu lieu ; elle ne dit pas qu'il a
    // produit son effet. Une action qui a PROMIS une verification (ADR 0131)
    // n'est donc close que verifiee, et le dossier ne se cloture pas avant.

    /** L'action qui a promis une verification, et qui est faite. */
    private CapaAction actionPromise(CapaCase c) {
        CapaAction a = action(c, CapaActionStatus.DONE);
        a.setVerificationRequired(true);
        a.setVerificationAssigneeId(UUID.randomUUID());
        a.setEffectivenessCriterion("Aucun rebut pendant 3 mois");
        return a;
    }

    private EffectivenessReview jugement(CapaCase c, CapaAction a, EffectivenessVerdict verdict) {
        return EffectivenessReview.recordForAction(UUID.randomUUID(), c.getTenantId(), c.getId(),
                a.getId(), verdict, "Aucun rebut pendant 3 mois", null, null,
                "0 rebut sur 3 mois", null, null, null, Instant.now());
    }

    @Test
    void verifyActionEffectiveness_enregistreLeJugementAvecLeCritereDeLAction() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = actionPromise(c);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));

        CapaDto.ActionResponse r = service.verifyActionEffectiveness(c.getId(), a.getId(),
                new CapaDto.EffectivenessRequest(null, EffectivenessVerdict.EFFECTIVE,
                        "4,2 %", "< 1 %", "0 rebut sur 3 mois", "Verifie sur trois cycles"));

        ArgumentCaptor<EffectivenessReview> ecrit = ArgumentCaptor.forClass(EffectivenessReview.class);
        verify(reviews).save(ecrit.capture());
        assertThat(ecrit.getValue().capaActionId()).isEqualTo(a.getId());
        assertThat(ecrit.getValue().capaCaseId()).isEqualTo(c.getId());
        assertThat(ecrit.getValue().criterion()).isEqualTo("Aucun rebut pendant 3 mois");
        assertThat(ecrit.getValue().observed()).isEqualTo("0 rebut sur 3 mois");
        assertThat(r.verificationVerdict()).isEqualTo(EffectivenessVerdict.EFFECTIVE);
        assertThat(r.verifiedAt()).isNotNull();
    }

    /**
     * Rien n'a ete promis sur cette action : il n'y a pas de critere auquel
     * confronter un releve, et juger sans critere annonce revient a choisir le
     * critere en regardant le resultat (ADR 0108).
     */
    @Test
    void verifyActionEffectiveness_refuseUneActionQuiNARienPromis() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = action(c, CapaActionStatus.DONE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.verifyActionEffectiveness(c.getId(), a.getId(),
                new CapaDto.EffectivenessRequest(null, EffectivenessVerdict.EFFECTIVE,
                        null, null, "0 rebut", null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("did not require");
        verify(reviews, never()).save(any());
    }

    /** On mesurerait l'effet d'un geste qui n'a pas eu lieu. */
    @Test
    void verifyActionEffectiveness_refuseUneActionPasEncoreFaite() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = actionPromise(c);
        a.setStatus(CapaActionStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.verifyActionEffectiveness(c.getId(), a.getId(),
                new CapaDto.EffectivenessRequest(null, EffectivenessVerdict.EFFECTIVE,
                        null, null, "0 rebut", null)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("not DONE");
    }

    @Test
    void verifyActionEffectiveness_refuseSansAucunCritere_niSurLActionNiSurLeDossier() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        CapaAction a = actionPromise(c);
        a.setEffectivenessCriterion(null);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.verifyActionEffectiveness(c.getId(), a.getId(),
                new CapaDto.EffectivenessRequest(null, EffectivenessVerdict.EFFECTIVE,
                        null, null, "0 rebut", null)))
                .isInstanceOf(CapaValidationException.class)
                .hasMessageContaining("No effectiveness criterion");
    }

    /** A defaut du sien, le critere du DOSSIER : le cas des actions anterieures. */
    @Test
    void verifyActionEffectiveness_retombeSurLeCritereDuDossier() {
        CapaCase c = capa(TENANT, CapaStatus.OPEN);
        c.setEffectivenessCriterion("Zero retard sur 620 expeditions");
        CapaAction a = actionPromise(c);
        a.setEffectivenessCriterion(null);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.findByIdAndCapaId(a.getId(), c.getId())).thenReturn(Optional.of(a));

        service.verifyActionEffectiveness(c.getId(), a.getId(),
                new CapaDto.EffectivenessRequest(null, EffectivenessVerdict.PARTIAL,
                        null, null, "3 retards", null));

        ArgumentCaptor<EffectivenessReview> ecrit = ArgumentCaptor.forClass(EffectivenessReview.class);
        verify(reviews).save(ecrit.capture());
        assertThat(ecrit.getValue().criterion()).isEqualTo("Zero retard sur 620 expeditions");
    }

    /**
     * LE REFUS DE CLOTURE. Le dossier ne se cloture pas tant qu'une action qui
     * a promis une verification ne l'a pas obtenue -- et un verdict PARTIEL ne
     * la leve pas : l'action a ete regardee, et ce qu'on a vu ne permet pas de
     * clore.
     */
    @Test
    void verifyEffectiveness_refuseDeCloreTantQuUneActionPromiseNEstPasVerifiee() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        CapaAction a = actionPromise(c);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        // Jamais jugee.
        assertThatThrownBy(() -> service.verifyEffectiveness(
                c.getId(), new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("promised an effectiveness verification");

        // Jugee insuffisante : l'obstacle demeure.
        when(reviews.findLastPerActionOfCase(TENANT, c.getId()))
                .thenReturn(Map.of(a.getId(), jugement(c, a, EffectivenessVerdict.PARTIAL)));
        assertThatThrownBy(() -> service.verifyEffectiveness(
                c.getId(), new CapaDto.EffectivenessRequest(true)))
                .isInstanceOf(CapaStateException.class)
                .hasMessageContaining("promised an effectiveness verification");
    }

    @Test
    void verifyEffectiveness_cloreDesQueChaqueActionPromiseEstJugeeEfficace() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        c.setEffectivenessCriterion("Aucun rebut pendant 3 mois");
        CapaAction a = actionPromise(c);
        action(c, CapaActionStatus.DONE, CapaActionType.PREVENTIVE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(caseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reviews.findLastPerActionOfCase(TENANT, c.getId()))
                .thenReturn(Map.of(a.getId(), jugement(c, a, EffectivenessVerdict.EFFECTIVE)));

        CapaDto.CaseResponse r = service.verifyEffectiveness(c.getId(),
                new CapaDto.EffectivenessRequest(null, EffectivenessVerdict.EFFECTIVE,
                        null, null, "0 rebut sur 3 mois", null));

        assertThat(r.status()).isEqualTo(CapaStatus.CLOSED);
    }

    /**
     * La fiche ANNONCE l'obstacle, et le serveur le tient : une liste qui
     * n'engage pas le serveur ne vaut rien, et un ecran qui n'annonce pas ce
     * que l'API refusera ne vaut pas mieux.
     */
    @Test
    void closureBlockers_annonceLesVerificationsDActionEnAttente() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        actionPromise(c);
        actionPromise(c);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        CapaDto.CaseResponse r = service.findById(c.getId());

        assertThat(r.closureBlockers())
                .extracting(CapaDto.ClosureBlocker::code, CapaDto.ClosureBlocker::count)
                .contains(tuple(ClosureBlockerCode.ACTION_VERIFICATIONS_PENDING, 2L));
    }

    /** Une action qui n'a RIEN promis n'entre pas dans le decompte. */
    @Test
    void closureBlockers_ignoreLesActionsQuiNOntRienPromis() {
        CapaCase c = capa(TENANT, CapaStatus.RESOLVED);
        action(c, CapaActionStatus.DONE);
        action(c, CapaActionStatus.DONE, CapaActionType.PREVENTIVE);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        CapaDto.CaseResponse r = service.findById(c.getId());

        assertThat(r.closureBlockers())
                .extracting(CapaDto.ClosureBlocker::code)
                .doesNotContain(ClosureBlockerCode.ACTION_VERIFICATIONS_PENDING);
    }
}
