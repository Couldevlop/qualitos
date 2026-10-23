package com.openlab.qualitos.quality.training.competencymatrix.application;

import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixConflictException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixNotFoundException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyMatrixValidationException;
import com.openlab.qualitos.quality.training.competencymatrix.domain.LevelCell;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixOperator;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Les cas d'usage de la matrice : le client et l'acteur du contexte, les
 * règles (atelier connu, doublons, formateur obligatoire, échelle), la trace
 * d'audit de chaque écriture — et rien d'écrit quand une règle refuse.
 */
class CompetencyMatrixServiceTest {

    static final Instant MAINTENANT = Instant.parse("2026-10-23T08:00:00Z");

    CompetencyMatrixPorts.Operators operators;
    CompetencyMatrixPorts.Skills skills;
    CompetencyMatrixPorts.Levels levels;
    CompetencyMatrixPorts.WorkshopSettings workshops;
    CompetencyMatrixPorts.Context context;
    CompetencyMatrixPorts.Audit audit;
    CompetencyMatrixService service;

    final UUID tenant = UUID.randomUUID();
    final UUID acteur = UUID.randomUUID();
    final UUID martin = UUID.randomUUID();
    final UUID formateur = UUID.randomUUID();
    final UUID tournage = UUID.randomUUID();

    @BeforeEach
    void setup() {
        operators = mock(CompetencyMatrixPorts.Operators.class);
        skills = mock(CompetencyMatrixPorts.Skills.class);
        levels = mock(CompetencyMatrixPorts.Levels.class);
        workshops = mock(CompetencyMatrixPorts.WorkshopSettings.class);
        context = mock(CompetencyMatrixPorts.Context.class);
        audit = mock(CompetencyMatrixPorts.Audit.class);
        when(context.requireTenantId()).thenReturn(tenant);
        when(context.requireActorId()).thenReturn(acteur);
        when(workshops.find(tenant)).thenReturn(Optional.empty());
        when(operators.findAll(tenant)).thenReturn(List.of());
        when(skills.findAll(tenant)).thenReturn(List.of());
        when(levels.findAll(tenant)).thenReturn(List.of());
        service = new CompetencyMatrixService(operators, skills, levels, workshops, context, audit,
                Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    }

    MatrixOperator op() {
        return new MatrixOperator(UUID.randomUUID(), martin, "Martin Dubois", "Usinage", MAINTENANT);
    }

    MatrixSkill sk() {
        return new MatrixSkill(tournage, "Tournage CNC", "Usinage", formateur, "Karim Benali", MAINTENANT);
    }

    // ---------------------------------------------------------------- lecture

    @Test
    void laMatriceSeLitDansLeClientDuJetonAvecSaCouverture() {
        when(operators.findAll(tenant)).thenReturn(List.of(op()));
        when(skills.findAll(tenant)).thenReturn(List.of(sk()));
        when(levels.findAll(tenant)).thenReturn(List.of(new LevelCell(martin, tournage, 3)));

        CompetencyMatrixDto.MatrixView v = service.view();

        assertThat(v.workshops()).containsExactly("Usinage", "Assemblage", "Qualité", "Maintenance");
        assertThat(v.defaultWorkshops()).isTrue();
        assertThat(v.operators()).singleElement().satisfies(o -> {
            assertThat(o.name()).isEqualTo("Martin Dubois");
            assertThat(o.workshop()).isEqualTo("Usinage");
        });
        assertThat(v.skills()).singleElement().satisfies(s -> {
            assertThat(s.trainerName()).isEqualTo("Karim Benali");
            assertThat(s.autonomous()).isEqualTo(1);
            assertThat(s.atRisk()).isTrue();
        });
        assertThat(v.cells()).containsExactly(new CompetencyMatrixDto.CellView(martin, tournage, 3));
    }

    @Test
    void lesAteliersReglesRemplacentCeuxDeLaPlateforme() {
        when(workshops.find(tenant)).thenReturn(Optional.of(List.of("Peinture")));
        CompetencyMatrixDto.WorkshopsView w = service.workshops();
        assertThat(w.names()).containsExactly("Peinture");
        assertThat(w.defaults()).isFalse();
        assertThat(service.view().defaultWorkshops()).isFalse();
    }

    @Test
    void uneListeVideEnBaseVautAbsenceDeReglage() {
        when(workshops.find(tenant)).thenReturn(Optional.of(List.of()));
        assertThat(service.workshops().defaults()).isTrue();
    }

    // ---------------------------------------------------------------- opérateurs

    @Test
    void ajouterUnOperateurLeRangeDansLAtelierCanoniqueEtTrace() {
        MatrixOperator ajoute = op();
        when(operators.findByUser(tenant, martin)).thenReturn(Optional.empty());
        when(operators.add(tenant, martin, "Martin Dubois", "Usinage", acteur, MAINTENANT)).thenReturn(ajoute);

        service.addOperator(new CompetencyMatrixDto.AddOperatorCommand(martin, "  Martin Dubois ", "usinage"));

        verify(operators).add(tenant, martin, "Martin Dubois", "Usinage", acteur, MAINTENANT);
        verify(audit).operatorAdded(tenant, ajoute, acteur);
    }

    @Test
    void unAtelierInconnuEstRefuse() {
        assertThatThrownBy(() -> service.addOperator(
                new CompetencyMatrixDto.AddOperatorCommand(martin, "Martin", "Peinture")))
                .isInstanceOf(CompetencyMatrixValidationException.class)
                .extracting(e -> ((CompetencyMatrixValidationException) e).getField()).isEqualTo("workshop");
        verify(operators, never()).add(any(), any(), any(), any(), any(), any());
        verifyNoInteractions(audit);
    }

    @Test
    void unNomVideEstRefuse() {
        assertThatThrownBy(() -> service.addOperator(
                new CompetencyMatrixDto.AddOperatorCommand(martin, "  ", "Usinage")))
                .isInstanceOf(CompetencyMatrixValidationException.class)
                .extracting(e -> ((CompetencyMatrixValidationException) e).getField()).isEqualTo("userName");
        assertThatThrownBy(() -> service.addOperator(
                new CompetencyMatrixDto.AddOperatorCommand(martin, null, "Usinage")))
                .isInstanceOf(CompetencyMatrixValidationException.class);
    }

    @Test
    void unePersonneDejaInscriteEstUnConflit() {
        when(operators.findByUser(tenant, martin)).thenReturn(Optional.of(op()));
        assertThatThrownBy(() -> service.addOperator(
                new CompetencyMatrixDto.AddOperatorCommand(martin, "Martin", "Usinage")))
                .isInstanceOf(CompetencyMatrixConflictException.class)
                .extracting(e -> ((CompetencyMatrixConflictException) e).getField()).isEqualTo("userId");
        verifyNoInteractions(audit);
    }

    @Test
    void retirerUnOperateurDuClientLeRetireEtTrace() {
        MatrixOperator o = op();
        when(operators.findById(tenant, o.id())).thenReturn(Optional.of(o));

        service.removeOperator(o.id());

        verify(operators).remove(tenant, o.id());
        verify(audit).operatorRemoved(tenant, o, acteur);
    }

    @Test
    void retirerUnOperateurInconnuDuClientRend404() {
        UUID id = UUID.randomUUID();
        when(operators.findById(tenant, id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeOperator(id)).isInstanceOf(CompetencyMatrixNotFoundException.class);
        verify(operators, never()).remove(any(), any());
    }

    // ---------------------------------------------------------------- compétences

    CompetencyMatrixDto.AddSkillCommand nouvelle() {
        return new CompetencyMatrixDto.AddSkillCommand(" Tournage CNC ", "Usinage", formateur, "Karim Benali");
    }

    @Test
    void uneCompetenceNouvelleEstCreeeAuCatalogueAvecSonFormateur() {
        when(skills.findCatalogueSkillByName(tenant, "Tournage CNC")).thenReturn(Optional.empty());
        when(skills.createCatalogueSkill(tenant, "Tournage CNC", "Usinage")).thenReturn(tournage);
        when(skills.add(tenant, tournage, formateur, "Karim Benali", acteur, MAINTENANT)).thenReturn(sk());

        service.addSkill(nouvelle());

        verify(skills).createCatalogueSkill(tenant, "Tournage CNC", "Usinage");
        verify(audit).skillAdded(tenant, sk(), acteur);
    }

    @Test
    void uneCompetenceDuCatalogueEstRepriseSansDoublon() {
        when(skills.findCatalogueSkillByName(tenant, "Tournage CNC")).thenReturn(Optional.of(tournage));
        when(skills.add(tenant, tournage, formateur, "Karim Benali", acteur, MAINTENANT)).thenReturn(sk());

        service.addSkill(nouvelle());

        verify(skills).categorizeIfMissing(tenant, tournage, "Usinage");
        verify(skills, never()).createCatalogueSkill(any(), any(), any());
    }

    @Test
    void uneCompetenceDejaEnColonneEstUnConflit() {
        when(skills.findAll(tenant)).thenReturn(List.of(sk()));
        assertThatThrownBy(() -> service.addSkill(
                new CompetencyMatrixDto.AddSkillCommand("tournage cnc", "Usinage", formateur, "Karim")))
                .isInstanceOf(CompetencyMatrixConflictException.class);
        verify(skills, never()).add(any(), any(), any(), any(), any(), any());
    }

    @Test
    void leFormateurEstObligatoire() {
        assertThatThrownBy(() -> service.addSkill(
                new CompetencyMatrixDto.AddSkillCommand("Tournage", "Usinage", null, "Karim")))
                .isInstanceOf(CompetencyMatrixValidationException.class)
                .extracting(e -> ((CompetencyMatrixValidationException) e).getField()).isEqualTo("trainerUserId");
        assertThatThrownBy(() -> service.addSkill(
                new CompetencyMatrixDto.AddSkillCommand("Tournage", "Usinage", formateur, " ")))
                .isInstanceOf(CompetencyMatrixValidationException.class)
                .extracting(e -> ((CompetencyMatrixValidationException) e).getField()).isEqualTo("trainerName");
        assertThatThrownBy(() -> service.addSkill(
                new CompetencyMatrixDto.AddSkillCommand("Tournage", "", formateur, "Karim")))
                .isInstanceOf(CompetencyMatrixValidationException.class)
                .extracting(e -> ((CompetencyMatrixValidationException) e).getField()).isEqualTo("category");
        verifyNoInteractions(audit);
    }

    @Test
    void retirerUneCompetenceLaLaisseAuCatalogueEtTrace() {
        when(skills.find(tenant, tournage)).thenReturn(Optional.of(sk()));
        service.removeSkill(tournage);
        verify(skills).remove(tenant, tournage);
        verify(audit).skillRemoved(tenant, sk(), acteur);
    }

    @Test
    void retirerUneCompetenceHorsMatriceRend404() {
        when(skills.find(tenant, tournage)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeSkill(tournage)).isInstanceOf(CompetencyMatrixNotFoundException.class);
        verify(skills, never()).remove(any(), any());
    }

    // ---------------------------------------------------------------- niveaux

    @Test
    void poserUnNiveauEvalueLaPersonneEtTraceLAncienNiveau() {
        when(operators.findByUser(tenant, martin)).thenReturn(Optional.of(op()));
        when(skills.find(tenant, tournage)).thenReturn(Optional.of(sk()));
        when(levels.set(tenant, martin, "Martin Dubois", tournage, 3, acteur, LocalDate.of(2026, 10, 23)))
                .thenReturn(OptionalInt.of(2));

        CompetencyMatrixDto.CellView c = service.setLevel(new CompetencyMatrixDto.SetLevelCommand(martin, tournage, 3));

        assertThat(c).isEqualTo(new CompetencyMatrixDto.CellView(martin, tournage, 3));
        verify(audit).levelSet(tenant, new LevelCell(martin, tournage, 3), OptionalInt.of(2), acteur);
    }

    @Test
    void unNiveauHorsEchelleNEcritRien() {
        assertThatThrownBy(() -> service.setLevel(new CompetencyMatrixDto.SetLevelCommand(martin, tournage, 5)))
                .isInstanceOf(CompetencyMatrixValidationException.class);
        verify(levels, never()).set(any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void unNiveauHorsMatriceRend404() {
        when(operators.findByUser(tenant, martin)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setLevel(new CompetencyMatrixDto.SetLevelCommand(martin, tournage, 2)))
                .isInstanceOf(CompetencyMatrixNotFoundException.class);

        when(operators.findByUser(tenant, martin)).thenReturn(Optional.of(op()));
        when(skills.find(tenant, tournage)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setLevel(new CompetencyMatrixDto.SetLevelCommand(martin, tournage, 2)))
                .isInstanceOf(CompetencyMatrixNotFoundException.class);
        verify(levels, never()).set(any(), any(), any(), any(), anyInt(), any(), any());
        verifyNoInteractions(audit);
    }

    // ---------------------------------------------------------------- ateliers

    @Test
    void remplacerLesAteliersLesNettoieLesEnregistreEtTrace() {
        CompetencyMatrixDto.WorkshopsView w = service.replaceWorkshops(List.of(" Peinture ", "Montage"));

        assertThat(w.names()).containsExactly("Peinture", "Montage");
        assertThat(w.defaults()).isFalse();
        verify(workshops).replace(tenant, List.of("Peinture", "Montage"), acteur);
        verify(audit).workshopsChanged(eq(tenant), eq(List.of("Peinture", "Montage")), eq(acteur));
    }

    @Test
    void desAteliersInvalidesNeSontPasEnregistres() {
        assertThatThrownBy(() -> service.replaceWorkshops(List.of("A", "a")))
                .isInstanceOf(CompetencyMatrixValidationException.class);
        verify(workshops, never()).replace(any(), any(), any());
        verifyNoInteractions(audit);
    }
}
