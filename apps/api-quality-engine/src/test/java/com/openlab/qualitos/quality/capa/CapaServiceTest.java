package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
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
        when(caseRepo.search(TENANT, CapaStatus.OPEN, CapaCriticity.CRITICAL, p))
                .thenReturn(new PageImpl<>(List.of(capa(TENANT, CapaStatus.OPEN))));
        assertThat(service.findAll(CapaStatus.OPEN, CapaCriticity.CRITICAL, p).getContent()).hasSize(1);
        verify(caseRepo, never()).findByTenantIdAndStatus(any(), any(), any());
        verify(caseRepo, never()).findByTenantId(any(), any());
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
                null, null, null, null, null);
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
                        null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    @Test
    void update_rejected_throws() {
        CapaCase c = capa(TENANT, CapaStatus.REJECTED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateCase(c.getId(),
                new CapaDto.UpdateCaseRequest("x", null, null, null, null, null,
                        null, null, null, null, null)))
                .isInstanceOf(CapaStateException.class);
    }

    // --- verification exigee ---

    /** Raccourci de lisibilite : seuls les quatre champs de verification varient. */
    private CapaDto.UpdateCaseRequest verification(Boolean exigee, UUID qui,
                                                   String nom, String consignes) {
        return new CapaDto.UpdateCaseRequest(null, null, null, null, null, null,
                exigee, qui, nom, consignes, null);
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
                "nouveau titre", null, null, null, null, null, null, null, null, null, null));

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
                null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null));

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
                null, null, CapaActionStatus.DONE, null, null, null, null, null));

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
                null, null, CapaActionStatus.DONE, null, null, null, null, null));

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
                null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null));

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
                "a faire", null, null, null, null, null, null, null));

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
                "complement", null, null, null, null, null, null, null));

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
                        null, null, null, null, null)))
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
                "fix", "desc", null, null, UUID.randomUUID(), null, null, LocalDate.now().plusDays(5));
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.PENDING);
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
                    CapaActionType.PREVENTIVE, null, null, null, null));

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
                "Autre personne", null, null));

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
                "Définir le plan", null, null, null, responsable, "  a.diallo@client.test  ", null, null));

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
                "Définir le plan", null, null, null, null, "Nom libre", null, null));

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
                "unique", null, null, null, null, null, null, null);
        assertThat(service.addAction(c.getId(), req).title()).isEqualTo("unique");
    }

    @Test
    void addAction_explicitStatus_used() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        CapaDto.ActionRequest req = new CapaDto.ActionRequest(
                "fix", null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null);
        CapaDto.ActionResponse r = service.addAction(c.getId(), req);
        assertThat(r.status()).isEqualTo(CapaActionStatus.IN_PROGRESS);
    }

    @Test
    void addAction_closed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null)))
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
                new CapaDto.ActionRequest("t", null, CapaActionStatus.DONE, null, null, null, null, null));

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
                new CapaDto.ActionRequest(null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null));

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
                new CapaDto.ActionRequest("nt", "nd", null, null, assignee, null, null, due));
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
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null)))
                .isInstanceOf(CapaActionNotFoundException.class);
    }

    @Test
    void updateAction_capaClosed_throws() {
        CapaCase c = capa(TENANT, CapaStatus.CLOSED);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.updateAction(c.getId(), UUID.randomUUID(),
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null)))
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
                null, null, CapaActionStatus.IN_PROGRESS, null, null, null, null, null));

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
                null, null, CapaActionStatus.DONE, null, null, null, null, null));

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
                        null, null, null, null, null)))
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
                "Réviser le plan de contrôle", null, null, null, null, "Amina Dridi", comite, null));

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
                new CapaDto.ActionRequest("x", null, null, null, null, null, null, null));

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
                new CapaDto.ActionRequest("x", null, null, null, null, "   ", null, null));

        // Sinon la colonne « Responsable » serait vide sans le « — » qui dit
        // qu'elle l'est : l'écran n'aurait plus qu'une seule chose à tester.
        assertThat(r.assigneeName()).isNull();
    }

    @Test
    void addAction_titreFaitDEspaces_estRefuse() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("   ", null, null, null, null, null, null, null)))
                .isInstanceOf(CapaValidationException.class);
        verify(actionRepo, never()).save(any());
    }

    @Test
    void addAction_titreTropLong_estRefuseSansTronquer() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.addAction(c.getId(),
                new CapaDto.ActionRequest("x".repeat(256), null, null, null, null, null, null, null)))
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
                        CapaActionStatus.IN_PROGRESS, null, null, null, null, null));

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
                new CapaDto.ActionRequest(" ", null, null, null, null, null, null, null)))
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
                null, null, CapaActionStatus.DONE, null, null, null, null, null));

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
                new CapaDto.ActionRequest("tri du lot", null, null, null, null, null, null, null));

        assertThat(r.actionType()).isEqualTo(CapaActionType.CORRECTIVE);
    }

    @Test
    void addAction_withContainment_keepsIt() {
        CapaCase c = capa(TENANT, CapaStatus.IN_PROGRESS);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(actionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapaDto.ActionResponse r = service.addAction(c.getId(), new CapaDto.ActionRequest(
                "tri du lot suspect", null, null, CapaActionType.CONTAINMENT,
                null, null, null, null));

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
                null, null, null, null, null, null, null, LocalDate.now().plusDays(3)));

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
                null, null, null, CapaActionType.PREVENTIVE, null, null, null, null));

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
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        action(c, CapaActionStatus.DONE, CapaActionType.CONTAINMENT);
        when(caseRepo.findByIdAndTenantId(c.getId(), TENANT)).thenReturn(Optional.of(c));
        when(ncRepo.countByTenantIdAndCapaCaseIdAndStatusNotIn(any(), any(), any())).thenReturn(0L);

        assertThat(service.findById(c.getId()).closureBlockers())
                .containsExactly(new CapaDto.ClosureBlocker(ClosureBlockerCode.CONTAINMENT_ONLY, 2));
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
                null, null, null, null, null, "  Moins de 2 % de rebuts sur 3 mois  "));

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
                        null, null, null, null, "un critere tout autre")))
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
                null, null, null, null, null, "Moins de 2 % de rebuts"));

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
}
