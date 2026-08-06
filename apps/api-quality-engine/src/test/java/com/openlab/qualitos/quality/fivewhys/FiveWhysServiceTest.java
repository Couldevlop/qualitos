package com.openlab.qualitos.quality.fivewhys;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.nonconformity.NcCategory;
import com.openlab.qualitos.quality.nonconformity.NcOrigin;
import com.openlab.qualitos.quality.nonconformity.NcSeverity;
import com.openlab.qualitos.quality.nonconformity.NcStatus;
import com.openlab.qualitos.quality.nonconformity.NonConformity;
import com.openlab.qualitos.quality.nonconformity.NonConformityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Analyse des 5 Pourquoi rattachée à une non-conformité (§3.5).
 *
 * <p>La méthode existait dans la plateforme comme sous-causes d'un diagramme
 * Ishikawa : imbriquée dans un arbre cause-effet, elle n'était ni identifiable,
 * ni consultable pour elle-même, et surtout on ne pouvait pas partir d'une
 * non-conformité pour la dérouler. Elle devient une analyse à part entière.
 *
 * <p>Cinq n'est pas un dogme : c'est un ordre de grandeur. Une chaîne de trois
 * pourquoi qui atteint la cause racine vaut mieux qu'une de cinq qui la dépasse,
 * et certaines défaillances en demandent sept. Le modèle porte donc une SUITE de
 * pourquoi, pas cinq colonnes figées.
 */
@ExtendWith(MockitoExtension.class)
class FiveWhysServiceTest {

    @Mock FiveWhysAnalysisRepository analysisRepo;
    @Mock FiveWhysStepRepository stepRepo;
    @Mock NonConformityRepository ncRepo;
    @InjectMocks FiveWhysService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID NC = UUID.randomUUID();

    @BeforeEach void ctx() { TenantContext.setTenantId(TENANT.toString()); }
    @AfterEach  void clr() { TenantContext.clear(); }

    private NonConformity nc() {
        NonConformity n = new NonConformity();
        n.setId(NC);
        n.setTenantId(TENANT);
        n.setReference("NC-2026-0007");
        n.setTitle("Étiquetage lot manquant");
        n.setCategory(NcCategory.PROCESS);
        n.setSeverity(NcSeverity.MAJOR);
        n.setStatus(NcStatus.OPEN);
        n.setOrigin(NcOrigin.INTERNAL);
        n.setDetectedAt(Instant.now());
        return n;
    }

    private FiveWhysAnalysis analysis() {
        FiveWhysAnalysis a = new FiveWhysAnalysis();
        a.setId(UUID.randomUUID());
        a.setTenantId(TENANT);
        a.setNonConformity(nc());
        a.setProblem("Étiquetage lot manquant");
        return a;
    }

    // ---- création -------------------------------------------------------------

    @Test
    @DisplayName("l'analyse part d'une non-conformité et en reprend l'énoncé")
    void startsFromANonConformity() {
        when(ncRepo.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));
        when(analysisRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FiveWhysDto.AnalysisResponse created = service.create(new FiveWhysDto.CreateRequest(NC, null));

        ArgumentCaptor<FiveWhysAnalysis> saved = ArgumentCaptor.forClass(FiveWhysAnalysis.class);
        verify(analysisRepo).save(saved.capture());
        // Reprendre le titre de la NC évite de retaper le problème, et garantit
        // que l'analyse parle bien du même écart que la non-conformité.
        assertThat(saved.getValue().getProblem()).isEqualTo("Étiquetage lot manquant");
        assertThat(created.ncReference()).isEqualTo("NC-2026-0007");
    }

    @Test
    @DisplayName("un énoncé explicite l'emporte sur le titre de la non-conformité")
    void anExplicitProblemWins() {
        when(ncRepo.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.of(nc()));
        when(analysisRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(new FiveWhysDto.CreateRequest(NC, "Palettes expédiées sans étiquette"));

        ArgumentCaptor<FiveWhysAnalysis> saved = ArgumentCaptor.forClass(FiveWhysAnalysis.class);
        verify(analysisRepo).save(saved.capture());
        assertThat(saved.getValue().getProblem()).isEqualTo("Palettes expédiées sans étiquette");
    }

    @Test
    @DisplayName("on n'analyse pas la non-conformité d'un autre tenant")
    void refusesAnotherTenantsNc() {
        when(ncRepo.findByIdAndTenantId(NC, TENANT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new FiveWhysDto.CreateRequest(NC, null)))
                .isInstanceOf(FiveWhysNotFoundException.class);
        verifyNoInteractions(analysisRepo);
    }

    @Test
    @DisplayName("sans tenant, rien n'est écrit")
    void refusesWithoutTenant() {
        TenantContext.clear();

        assertThatThrownBy(() -> service.create(new FiveWhysDto.CreateRequest(NC, null)))
                .isInstanceOf(MissingTenantContextException.class);
        verifyNoInteractions(analysisRepo);
    }

    // ---- chaîne de pourquoi ----------------------------------------------------

    @Test
    @DisplayName("chaque pourquoi s'ajoute à la suite du précédent")
    void appendsWhysInOrder() {
        FiveWhysAnalysis a = analysis();
        when(analysisRepo.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(stepRepo.countByAnalysisIdAndTenantId(a.getId(), TENANT)).thenReturn(2L);
        when(stepRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.addStep(a.getId(), new FiveWhysDto.AddStepRequest("Le rouleau était vide"));

        ArgumentCaptor<FiveWhysStep> saved = ArgumentCaptor.forClass(FiveWhysStep.class);
        verify(stepRepo).save(saved.capture());
        assertThat(saved.getValue().getPosition()).isEqualTo(3);
        assertThat(saved.getValue().getAnswer()).isEqualTo("Le rouleau était vide");
    }

    @Test
    @DisplayName("la chaîne s'arrête à sept pourquoi")
    void stopsAtSevenWhys() {
        // Au-delà, on ne remonte plus une cause : on énumère des circonstances.
        // La borne protège d'une liste qui s'allonge sans jamais conclure.
        FiveWhysAnalysis a = analysis();
        when(analysisRepo.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(stepRepo.countByAnalysisIdAndTenantId(a.getId(), TENANT)).thenReturn(7L);

        assertThatThrownBy(() -> service.addStep(a.getId(),
                new FiveWhysDto.AddStepRequest("Un huitième pourquoi")))
                .isInstanceOf(FiveWhysStateException.class);
    }

    @Test
    @DisplayName("une réponse vide est refusée")
    void refusesAnEmptyAnswer() {
        FiveWhysAnalysis a = analysis();
        when(analysisRepo.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.addStep(a.getId(),
                new FiveWhysDto.AddStepRequest("   ")))
                .isInstanceOf(FiveWhysStateException.class);
    }

    @Test
    @DisplayName("un pourquoi se corrige sans toucher aux autres")
    void editsOneWhy() {
        FiveWhysStep step = new FiveWhysStep();
        step.setId(UUID.randomUUID());
        step.setTenantId(TENANT);
        step.setAnalysis(analysis());
        step.setPosition(2);
        step.setAnswer("Ancienne réponse");
        when(stepRepo.findByIdAndTenantId(step.getId(), TENANT)).thenReturn(Optional.of(step));
        when(stepRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStep(step.getId(), new FiveWhysDto.AddStepRequest("Nouvelle réponse"));

        assertThat(step.getAnswer()).isEqualTo("Nouvelle réponse");
        assertThat(step.getPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("supprimer le dernier pourquoi est permis, pas un pourquoi du milieu")
    void onlyTheLastWhyCanBeRemoved() {
        // Retirer un maillon du milieu casserait la chaîne : le pourquoi suivant
        // ne répondrait plus à celui qui le précède.
        FiveWhysAnalysis a = analysis();
        FiveWhysStep middle = new FiveWhysStep();
        middle.setId(UUID.randomUUID());
        middle.setTenantId(TENANT);
        middle.setAnalysis(a);
        middle.setPosition(2);
        middle.setAnswer("Au milieu");
        when(stepRepo.findByIdAndTenantId(middle.getId(), TENANT)).thenReturn(Optional.of(middle));
        when(stepRepo.countByAnalysisIdAndTenantId(a.getId(), TENANT)).thenReturn(4L);

        assertThatThrownBy(() -> service.deleteStep(middle.getId()))
                .isInstanceOf(FiveWhysStateException.class);
    }

    @Test
    @DisplayName("le dernier pourquoi se retire")
    void removesTheLastWhy() {
        FiveWhysAnalysis a = analysis();
        FiveWhysStep last = new FiveWhysStep();
        last.setId(UUID.randomUUID());
        last.setTenantId(TENANT);
        last.setAnalysis(a);
        last.setPosition(4);
        last.setAnswer("Le dernier");
        when(stepRepo.findByIdAndTenantId(last.getId(), TENANT)).thenReturn(Optional.of(last));
        when(stepRepo.countByAnalysisIdAndTenantId(a.getId(), TENANT)).thenReturn(4L);

        service.deleteStep(last.getId());

        verify(stepRepo).delete(last);
    }

    // ---- cause racine -----------------------------------------------------------

    @Test
    @DisplayName("la cause racine ne se conclut pas avant trois pourquoi")
    void refusesARootCauseTooEarly() {
        // Conclure au deuxième pourquoi, c'est nommer un symptôme. La méthode
        // n'a d'intérêt que si l'on est allé au-delà de la première explication.
        FiveWhysAnalysis a = analysis();
        when(analysisRepo.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(stepRepo.countByAnalysisIdAndTenantId(a.getId(), TENANT)).thenReturn(2L);

        assertThatThrownBy(() -> service.setRootCause(a.getId(),
                new FiveWhysDto.RootCauseRequest("Procédure d'étiquetage non appliquée")))
                .isInstanceOf(FiveWhysStateException.class);
    }

    @Test
    @DisplayName("la cause racine se conclut à partir de trois pourquoi")
    void acceptsARootCause() {
        FiveWhysAnalysis a = analysis();
        when(analysisRepo.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(stepRepo.countByAnalysisIdAndTenantId(a.getId(), TENANT)).thenReturn(3L);
        when(analysisRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.setRootCause(a.getId(),
                new FiveWhysDto.RootCauseRequest("Procédure d'étiquetage non appliquée"));

        assertThat(a.getRootCause()).isEqualTo("Procédure d'étiquetage non appliquée");
    }

    @Test
    @DisplayName("l'analyse se relit avec toute sa chaîne")
    void readsBackTheWholeChain() {
        FiveWhysAnalysis a = analysis();
        FiveWhysStep s1 = new FiveWhysStep();
        s1.setId(UUID.randomUUID()); s1.setTenantId(TENANT); s1.setAnalysis(a);
        s1.setPosition(1); s1.setAnswer("Premier pourquoi");
        when(analysisRepo.findByIdAndTenantId(a.getId(), TENANT)).thenReturn(Optional.of(a));
        when(stepRepo.findByAnalysisIdAndTenantIdOrderByPositionAsc(a.getId(), TENANT))
                .thenReturn(List.of(s1));

        FiveWhysDto.AnalysisResponse read = service.get(a.getId());

        assertThat(read.steps()).hasSize(1);
        assertThat(read.steps().get(0).answer()).isEqualTo("Premier pourquoi");
    }
}
