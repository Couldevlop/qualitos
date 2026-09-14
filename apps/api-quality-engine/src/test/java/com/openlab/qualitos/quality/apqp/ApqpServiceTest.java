package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le cycle APQP d'un PROJET.
 *
 * <p>Trois exigences se croisent. Le cycle est AMORCÉ depuis le référentiel mais
 * appartient ensuite au client. La forme en V doit tenir quel que soit le nombre
 * de phases — c'était la condition pour qu'on puisse en ajouter. Et depuis l'ADR
 * 0072, la CASE pilote : le statut et l'avancement la suivent, sans quoi l'écran
 * afficherait « non démarré » sur un livrable coché.
 */
@ExtendWith(MockitoExtension.class)
class ApqpServiceTest {

    @Mock ApqpPhaseRepository repository;
    @Mock ApqpProjectRepository projets;
    @Mock ApqpDeliverableEvidenceRepository evidences;
    @Mock ApqpLinkResolver linkResolver;
    @Mock ApqpCycleSeeder seeder;
    ApqpService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PROJET_ID = UUID.randomUUID();
    static final UUID PHASE_ID = UUID.randomUUID();
    static final UUID LIVRABLE_ID = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();

    @BeforeEach
    void poserLeTenant() {
        TenantContext.setTenantId(TENANT.toString());
        service = new ApqpService(repository, projets, evidences, linkResolver, seeder);
    }

    @AfterEach
    void rendreLeTenant() {
        TenantContext.clear();
        LocaleContextHolder.resetLocaleContext();
    }

    // ---------- le V ----------

    @Test
    @DisplayName("le V descend jusqu'au milieu puis remonte, quel que soit le nombre de phases")
    void leVGardeSaForme() {
        assertThat(List.of(1, 2, 3, 4, 5).stream().map(i -> ApqpService.niveau(i, 5)).toList())
                .containsExactly(1, 2, 3, 2, 1);
        assertThat(List.of(1, 2, 3, 4, 5, 6).stream().map(i -> ApqpService.niveau(i, 6)).toList())
                .containsExactly(1, 2, 3, 3, 2, 1);
        // Une phase seule est à la fois l'entrée et le point bas : sans ce cas, le
        // premier ajout après une suppression totale dessinerait un V sans sommet.
        assertThat(ApqpService.niveau(1, 1)).isEqualTo(1);
    }

    // ---------- amorçage ----------

    @Test
    @DisplayName("un projet sans phase reçoit le cycle du référentiel à la première lecture")
    void lePremierRegardAmorce() {
        ApqpProject projet = projet();
        when(projets.findByIdAndTenantId(PROJET_ID, TENANT)).thenReturn(Optional.of(projet));
        when(repository.existsByProjectIdAndTenantId(PROJET_ID, TENANT)).thenReturn(false);
        when(repository.findByProjectIdAndTenantIdOrderByPositionAsc(PROJET_ID, TENANT))
                .thenReturn(List.of());
        when(evidences.countByDeliverableForTenant(TENANT)).thenReturn(List.of());

        service.cycle(PROJET_ID);

        verify(seeder).amorcer(projet);
    }

    @Test
    @DisplayName("un projet qui a déjà son cycle n'est pas réamorcé")
    void onNAmorcePasDeuxFois() {
        preparerCycle(livrable());

        service.cycle(PROJET_ID);

        verify(seeder, never()).amorcer(any());
    }

    // ---------- isolement : le projet d'un autre client ----------

    @Test
    @DisplayName("le projet d'un autre client est introuvable, pas interdit")
    void leProjetDUnAutreClientResteInvisible() {
        when(projets.findByIdAndTenantId(PROJET_ID, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cycle(PROJET_ID))
                .isInstanceOf(ApqpProjectNotFoundException.class);
    }

    @Test
    @DisplayName("sans tenant au contexte, rien ne se lit")
    void sansTenantRienNeSeLit() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.cycle(PROJET_ID))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    @DisplayName("une phase d'un AUTRE projet du même client est introuvable")
    void laPhaseDUnAutreProjetResteInvisible() {
        when(projets.findByIdAndTenantId(PROJET_ID, TENANT)).thenReturn(Optional.of(projet()));
        when(repository.findByIdAndProjectIdAndTenantId(PHASE_ID, PROJET_ID, TENANT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completerLivrable(
                PROJET_ID, PHASE_ID, LIVRABLE_ID, coche(true), ACTEUR))
                .isInstanceOf(ApqpPhaseNotFoundException.class);
    }

    // ---------- la case pilote (ADR 0072) ----------

    @Test
    @DisplayName("cocher pose « terminé » et 100 %, quoi qu'on ait envoyé d'autre")
    void cocherPoseTermineEtCent() {
        ApqpDeliverable livrable = livrable();
        livrable.setStatus(ApqpDeliverableStatus.BLOCKED);
        livrable.setPercentComplete(20);
        preparerCycle(livrable);

        // Le corps ment délibérément : statut bloqué, avancement 20. La case gagne.
        service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(true, null, null, null, null,
                        ApqpDeliverableStatus.BLOCKED, 20, null, null, null), ACTEUR);

        assertThat(livrable.isDone()).isTrue();
        assertThat(livrable.getStatus()).isEqualTo(ApqpDeliverableStatus.DONE);
        assertThat(livrable.getPercentComplete()).isEqualTo(100);
        assertThat(livrable.getDoneAt()).isNotNull();
        assertThat(livrable.getDoneBy()).isEqualTo(ACTEUR);
    }

    @Test
    @DisplayName("décocher depuis la liste remet l'avancement en arrière")
    void decocherRemetEnArriere() {
        ApqpDeliverable livrable = livrable();
        livrable.setDone(true);
        livrable.setStatus(ApqpDeliverableStatus.DONE);
        livrable.setPercentComplete(100);
        livrable.setDoneBy(ACTEUR);
        preparerCycle(livrable);

        // La liste n'a pas de formulaire : ni statut ni avancement dans le corps.
        service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID, coche(false), ACTEUR);

        assertThat(livrable.isDone()).isFalse();
        assertThat(livrable.getStatus()).isEqualTo(ApqpDeliverableStatus.IN_PROGRESS);
        assertThat(livrable.getPercentComplete()).isZero();
        // Garder la trace d'un achèvement retiré la rendrait fausse.
        assertThat(livrable.getDoneAt()).isNull();
        assertThat(livrable.getDoneBy()).isNull();
    }

    @Test
    @DisplayName("décocher en choisissant « bloqué » à 40 % respecte ce choix")
    void decocherRespecteLeStatutChoisi() {
        ApqpDeliverable livrable = livrable();
        livrable.setDone(true);
        preparerCycle(livrable);

        service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(false, null, null, null, null,
                        ApqpDeliverableStatus.BLOCKED, 40, null, null, null), ACTEUR);

        assertThat(livrable.getStatus()).isEqualTo(ApqpDeliverableStatus.BLOCKED);
        assertThat(livrable.getPercentComplete()).isEqualTo(40);
    }

    @Test
    @DisplayName("« terminé » sans la case retombe sur « en cours » : une seule vérité")
    void leStatutTermineSansLaCaseNeTientPas() {
        ApqpDeliverable livrable = livrable();
        preparerCycle(livrable);

        service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(false, null, null, null, null,
                        ApqpDeliverableStatus.DONE, 100, null, null, null), ACTEUR);

        assertThat(livrable.isDone()).isFalse();
        assertThat(livrable.getStatus()).isEqualTo(ApqpDeliverableStatus.IN_PROGRESS);
        assertThat(livrable.getPercentComplete()).isZero();
    }

    // ---------- les colonnes du classeur ----------

    @Test
    @DisplayName("le formulaire unique enregistre les colonnes D à J")
    void leFormulaireUniqueEcritToutesLesColonnes() {
        ApqpDeliverable livrable = livrable();
        preparerCycle(livrable);

        service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(false, "  Rapport de R&R signé  ", true,
                        "  A. Dupont  ", LocalDate.of(2026, 11, 30),
                        ApqpDeliverableStatus.IN_PROGRESS, 60, "  reçu le 3  ", null, null),
                ACTEUR);

        assertThat(livrable.getExpectedArtifact()).isEqualTo("Rapport de R&R signé");
        assertThat(livrable.isPpap()).isTrue();
        assertThat(livrable.getOwner()).isEqualTo("A. Dupont");
        assertThat(livrable.getDueDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(livrable.getPercentComplete()).isEqualTo(60);
        assertThat(livrable.getComment()).isEqualTo("reçu le 3");
    }

    @Test
    @DisplayName("la marque PPAP absente du corps ne l'efface pas")
    void laMarquePpapAbsenteNeSEffacePas() {
        ApqpDeliverable livrable = livrable();
        livrable.setPpap(true);
        preparerCycle(livrable);

        // C'est le cas de la case cochée depuis la liste, qui n'envoie pas la marque.
        service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID, coche(true), ACTEUR);

        assertThat(livrable.isPpap()).isTrue();
    }

    // ---------- le renvoi, devenu facultatif ----------

    @Test
    @DisplayName("tout livrable peut désigner un enregistrement, et il est vérifié dans le tenant")
    void leRenvoiEstVerifie() {
        ApqpDeliverable livrable = livrable();
        preparerCycle(livrable);
        UUID amdec = UUID.randomUUID();

        service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(true, null, null, null, null, null, null, null,
                        ApqpLinkedKind.FMEA, amdec), ACTEUR);

        verify(linkResolver).verifier(ApqpLinkedKind.FMEA, amdec, TENANT);
        assertThat(livrable.getLinkedId()).isEqualTo(amdec);
    }

    @Test
    @DisplayName("un renvoi à moitié posé est refusé : il n'ouvrirait rien")
    void leRenvoiAMoitiePoseEstRefuse() {
        preparerCycle(livrable());

        assertThatThrownBy(() -> service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(false, null, null, null, null, null, null, null,
                        ApqpLinkedKind.FMEA, null), ACTEUR))
                .isInstanceOf(ApqpDeliverableValidationException.class);
    }

    @Test
    @DisplayName("un renvoi mort ne laisse pas le livrable coché à moitié")
    void unRenvoiMortNeTouchePasAuLivrable() {
        ApqpDeliverable livrable = livrable();
        preparerCycle(livrable);
        UUID fantome = UUID.randomUUID();
        doThrow(new ApqpDeliverableValidationException("absent"))
                .when(linkResolver).verifier(ApqpLinkedKind.CAPA, fantome, TENANT);

        assertThatThrownBy(() -> service.completerLivrable(PROJET_ID, PHASE_ID, LIVRABLE_ID,
                new ApqpDto.CompletionRequest(true, null, null, null, null, null, null, null,
                        ApqpLinkedKind.CAPA, fantome), ACTEUR))
                .isInstanceOf(ApqpDeliverableValidationException.class);

        assertThat(livrable.isDone()).isFalse();
        assertThat(livrable.getLinkedId()).isNull();
    }

    // ---------- l'artefact attendu ----------

    @Test
    @DisplayName("un livrable sans artefact stocké rend celui du référentiel, dans la langue lue")
    void lArtefactVideRetombeSurLeReferentiel() {
        ApqpDeliverable livrable = livrable();
        livrable.setReferenceKey("deliv.pfmea");
        livrable.setExpectedArtifact(null);
        ApqpPhase phase = preparerCycle(livrable);
        phase.setReferenceKey("phase.process-design");
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ApqpDto.CycleResponse cycle = service.cycle(PROJET_ID);

        assertThat(cycle.phases().get(0).deliverables().get(0).expectedArtifact())
                .isEqualTo("Process FMEA linked to DFMEA/process flow, RPN/AP ranking, action plan");
    }

    @Test
    @DisplayName("« Control plan » prend l'artefact de SA phase, pas celui de l'autre")
    void lePlanDeSurveillanceNeConfondPasSesDeuxEtats() {
        ApqpDeliverable livrable = livrable();
        livrable.setReferenceKey("deliv.control-plan");
        livrable.setExpectedArtifact(null);
        ApqpPhase phase = preparerCycle(livrable);
        // La phase de VALIDATION, où le plan est celui de production — alors que
        // la première correspondance du référentiel est celui de pré-lancement.
        phase.setReferenceKey("phase.validation");
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ApqpDto.CycleResponse cycle = service.cycle(PROJET_ID);

        assertThat(cycle.phases().get(0).deliverables().get(0).expectedArtifact())
                .isEqualTo("Production Control Plan (finalized, post run-at-rate)");
    }

    @Test
    @DisplayName("un artefact réécrit par le client ne se traduit plus")
    void lArtefactReecritAppartientAuClient() {
        ApqpDeliverable livrable = livrable();
        livrable.setReferenceKey("deliv.pfmea");
        livrable.setExpectedArtifact("Notre AMDEC maison, feuille 3");
        ApqpPhase phase = preparerCycle(livrable);
        phase.setReferenceKey("phase.process-design");
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        ApqpDto.CycleResponse cycle = service.cycle(PROJET_ID);

        assertThat(cycle.phases().get(0).deliverables().get(0).expectedArtifact())
                .isEqualTo("Notre AMDEC maison, feuille 3");
    }

    // ---------- le dossier PPAP ----------

    @Test
    @DisplayName("le compte du dossier PPAP est calculé par le serveur, pas par l'écran")
    void leDossierPpapSeCompteAuServeur() {
        ApqpDeliverable requis = livrable();
        requis.setPpap(true);
        requis.setDone(true);
        ApqpDeliverable requisNonFourni = new ApqpDeliverable();
        requisNonFourni.setId(UUID.randomUUID());
        requisNonFourni.setLabel("FAIR");
        requisNonFourni.setPosition(2);
        requisNonFourni.setPpap(true);
        ApqpDeliverable horsDossier = new ApqpDeliverable();
        horsDossier.setId(UUID.randomUUID());
        horsDossier.setLabel("Plan projet");
        horsDossier.setPosition(3);

        preparerCycle(requis, requisNonFourni, horsDossier);

        ApqpDto.CycleResponse cycle = service.cycle(PROJET_ID);

        assertThat(cycle.ppapTotal()).isEqualTo(2);
        assertThat(cycle.ppapDone()).isEqualTo(1);
        assertThat(cycle.projectName()).isEqualTo("Programme X");
    }

    // ---------- réorganisation ----------

    @Test
    @DisplayName("un ordre partiel est refusé : il laisserait des phases hors du V")
    void unOrdrePartielEstRefuse() {
        ApqpPhase une = phase(PHASE_ID, 1);
        ApqpPhase deux = phase(UUID.randomUUID(), 2);
        when(projets.findByIdAndTenantId(PROJET_ID, TENANT)).thenReturn(Optional.of(projet()));
        when(repository.findByProjectIdAndTenantIdOrderByPositionAsc(PROJET_ID, TENANT))
                .thenReturn(List.of(une, deux));

        assertThatThrownBy(() -> service.reorganiser(PROJET_ID,
                new ApqpDto.ReorderRequest(List.of(une.getId()))))
                .isInstanceOf(ApqpReorderException.class);
    }

    // ---------- réinitialisation ----------

    @Test
    @DisplayName("réinitialiser efface le cycle du projet, et de lui seul")
    void reinitialiserNeToucheQuAuProjetVise() {
        ApqpProject projet = projet();
        when(projets.findByIdAndTenantId(PROJET_ID, TENANT)).thenReturn(Optional.of(projet));
        when(repository.findByProjectIdAndTenantIdOrderByPositionAsc(PROJET_ID, TENANT))
                .thenReturn(List.of());
        when(evidences.countByDeliverableForTenant(TENANT)).thenReturn(List.of());

        service.reinitialiser(PROJET_ID);

        verify(repository).deleteByProjectIdAndTenantId(PROJET_ID, TENANT);
        verify(seeder).amorcer(projet);
    }

    // ---------- outillage ----------

    private static ApqpDto.CompletionRequest coche(boolean fait) {
        return new ApqpDto.CompletionRequest(
                fait, null, null, null, null, null, null, null, null, null);
    }

    private ApqpProject projet() {
        ApqpProject projet = new ApqpProject();
        projet.setId(PROJET_ID);
        projet.setTenantId(TENANT);
        projet.setName("Programme X");
        projet.setType(ApqpProjectType.NPI);
        return projet;
    }

    private ApqpPhase phase(UUID id, int rang) {
        ApqpPhase phase = new ApqpPhase();
        phase.setId(id);
        phase.setTenantId(TENANT);
        phase.setPosition(rang);
        phase.setTitle("Phase " + rang);
        phase.setDeliverables(new ArrayList<>());
        return phase;
    }

    private ApqpDeliverable livrable() {
        ApqpDeliverable livrable = new ApqpDeliverable();
        livrable.setId(LIVRABLE_ID);
        livrable.setTenantId(TENANT);
        livrable.setLabel("AMDEC processus (PFMEA)");
        livrable.setPosition(1);
        return livrable;
    }

    /** Un projet, une phase, ses livrables — et les dépôts qui les rendent. */
    private ApqpPhase preparerCycle(ApqpDeliverable... livrables) {
        ApqpProject projet = projet();
        ApqpPhase phase = phase(PHASE_ID, 1);
        for (ApqpDeliverable livrable : livrables) {
            phase.addDeliverable(livrable);
        }
        when(projets.findByIdAndTenantId(PROJET_ID, TENANT)).thenReturn(Optional.of(projet));
        lenient().when(repository.existsByProjectIdAndTenantId(PROJET_ID, TENANT))
                .thenReturn(true);
        lenient().when(repository.findByIdAndProjectIdAndTenantId(PHASE_ID, PROJET_ID, TENANT))
                .thenReturn(Optional.of(phase));
        lenient().when(repository.findByProjectIdAndTenantIdOrderByPositionAsc(PROJET_ID, TENANT))
                .thenReturn(List.of(phase));
        lenient().when(evidences.countByDeliverableForTenant(TENANT)).thenReturn(List.of());
        return phase;
    }
}
