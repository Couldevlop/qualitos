package com.openlab.qualitos.quality.training.competencymatrix.application;

import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La lecture de la matrice de compétences.
 *
 * <p>Le service ne fait presque rien, et c'est voulu : l'assemblage vit dans le
 * domaine, où il se teste sans base ni conteneur. Ce banc vérifie ce qui reste —
 * que le tenant vient du contexte de sécurité, et que la grille se réassemble à
 * chaque appel plutôt que d'être mise en cache.
 */
class CompetencyMatrixServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID SKILL = UUID.randomUUID();
    static final UUID ANNA = UUID.randomUUID();

    SkillCataloguePort skills;
    CompetencyAssessmentPort assessments;
    TenantProvider tenants;
    CompetencyMatrixService service;

    @BeforeEach
    void setUp() {
        skills = mock(SkillCataloguePort.class);
        assessments = mock(CompetencyAssessmentPort.class);
        tenants = mock(TenantProvider.class);
        when(tenants.requireTenantId()).thenReturn(TENANT);
        service = new CompetencyMatrixService(skills, assessments, tenants);
    }

    @Test
    void itAssemblesTheCatalogueAndTheAssessmentsOfTheTenant() {
        when(skills.findAll(TENANT)).thenReturn(List.of(
                new CompetencyGrid.SkillEntry(SKILL, "PLAN", "Planification", "Gestion de projet")));
        when(assessments.findAll(TENANT)).thenReturn(List.of(
                new CompetencyGrid.Assessment(ANNA, "Anna", SKILL, 4)));

        CompetencyGrid grid = service.grid();

        assertThat(grid.groups()).hasSize(1);
        assertThat(grid.groups().get(0).category()).isEqualTo("Gestion de projet");
        assertThat(grid.people()).extracting(CompetencyGrid.Person::label).containsExactly("Anna");
        assertThat(grid.groups().get(0).rows().get(0).levels()).containsExactly(4);
    }

    @Test
    void theTenantComesFromTheSecurityContextAndNowhereElse() {
        when(skills.findAll(TENANT)).thenReturn(List.of());
        when(assessments.findAll(TENANT)).thenReturn(List.of());

        service.grid();

        verify(tenants).requireTenantId();
        verify(skills).findAll(TENANT);
        verify(assessments).findAll(TENANT);
    }

    @Test
    void anEmptyCatalogueYieldsAnEmptyGridRatherThanAnError() {
        when(skills.findAll(TENANT)).thenReturn(List.of());
        when(assessments.findAll(TENANT)).thenReturn(List.of());

        CompetencyGrid grid = service.grid();

        assertThat(grid.groups()).isEmpty();
        assertThat(grid.people()).isEmpty();
    }

    /**
     * Une matrice mise en cache mentirait dès l'évaluation suivante : c'est un
     * écran qu'on ouvre justement après avoir évalué quelqu'un.
     */
    @Test
    void theGridIsReassembledAtEachCall() {
        when(skills.findAll(TENANT)).thenReturn(List.of());
        when(assessments.findAll(TENANT)).thenReturn(List.of());

        service.grid();
        service.grid();

        verify(assessments, org.mockito.Mockito.times(2)).findAll(TENANT);
    }

    /**
     * Cinq cents compétences sur cent collaborateurs, c'est cinquante mille
     * cellules : l'assemblage doit rester linéaire. Une implémentation qui
     * chercherait la personne par parcours de liste à chaque cellule tiendrait
     * sur un jeu de démonstration et s'effondrerait ici.
     */
    @Test
    @Timeout(5)
    void aLargeGridIsAssembledWithoutQuadraticCost() {
        List<CompetencyGrid.SkillEntry> catalogue = new ArrayList<>();
        List<CompetencyGrid.Assessment> notes = new ArrayList<>();
        List<UUID> gens = new ArrayList<>();
        for (int p = 0; p < 100; p++) {
            gens.add(UUID.randomUUID());
        }
        for (int s = 0; s < 500; s++) {
            UUID skillId = UUID.randomUUID();
            catalogue.add(new CompetencyGrid.SkillEntry(skillId, "S" + s, "Compétence " + s, "Groupe " + (s % 10)));
            for (int p = 0; p < 100; p++) {
                notes.add(new CompetencyGrid.Assessment(gens.get(p), "Personne " + p, skillId, p % 5));
            }
        }
        when(skills.findAll(TENANT)).thenReturn(catalogue);
        when(assessments.findAll(TENANT)).thenReturn(notes);

        CompetencyGrid grid = service.grid();

        assertThat(grid.people()).hasSize(100);
        assertThat(grid.groups()).hasSize(10);
        assertThat(grid.groups().get(0).rows().get(0).levels()).hasSize(100);
    }
}
