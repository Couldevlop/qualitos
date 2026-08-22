package com.openlab.qualitos.quality.training.competencymatrix.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La matrice de compétences : compétences en lignes, groupées ; collaborateurs
 * en colonnes ; un niveau à l'intersection.
 *
 * <p>Ce que cette figure affirme, et qu'une liste ne dit pas : elle se lit dans
 * les DEUX sens. Une ligne montre qui sait faire quoi — et donc si une
 * compétence ne tient qu'à une personne. Une colonne montre ce qu'une personne
 * couvre. C'est la lecture en ligne qui intéresse le qualiticien : une case
 * unique sur toute une rangée est un risque, pas une donnée.
 *
 * <p>Deux refus que ce banc protège : une case jamais évaluée reste VIDE — la
 * confondre avec un niveau zéro reviendrait à affirmer une incompétence qu'on
 * n'a jamais constatée — et l'ordre des colonnes ne dépend pas de l'ordre où la
 * base a rendu les évaluations, sans quoi la matrice changerait de forme à
 * chaque rafraîchissement.
 */
class CompetencyGridTest {

    static final UUID ANNA = UUID.randomUUID();
    static final UUID BORIS = UUID.randomUUID();
    static final UUID PLANIF = UUID.randomUUID();
    static final UUID RISQUES = UUID.randomUUID();
    static final UUID LEADERSHIP = UUID.randomUUID();

    @Test
    void itGroupsSkillsByTheirCategory() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Gestion de projet"),
                        skill(RISQUES, "RISK", "Gestion des risques", "Gestion de projet"),
                        skill(LEADERSHIP, "LEAD", "Leadership", "Savoir-être")),
                List.of(assess(ANNA, "Anna", PLANIF, 4)));

        assertThat(grid.groups()).extracting(CompetencyGrid.Group::category)
                .containsExactly("Gestion de projet", "Savoir-être");
        assertThat(grid.groups().get(0).rows()).hasSize(2);
    }

    @Test
    void skillsWithoutACategoryComeLast() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", null),
                        skill(LEADERSHIP, "LEAD", "Leadership", "Savoir-être")),
                List.of());

        assertThat(grid.groups()).extracting(CompetencyGrid.Group::category)
                .containsExactly("Savoir-être", null);
    }

    @Test
    void withinAGroupSkillsFollowTheirName() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(RISQUES, "RISK", "Zonage", "Groupe"),
                        skill(PLANIF, "PLAN", "Alésage", "Groupe")),
                List.of());

        assertThat(grid.groups().get(0).rows()).extracting(CompetencyGrid.Row::name)
                .containsExactly("Alésage", "Zonage");
    }

    @Test
    void theColumnsAreThePeopleWhoHaveBeenAssessed() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Groupe")),
                List.of(assess(BORIS, "Boris", PLANIF, 2), assess(ANNA, "Anna", PLANIF, 4)));

        assertThat(grid.people()).extracting(CompetencyGrid.Person::label)
                .containsExactly("Anna", "Boris");
    }

    @Test
    void aCellNeverAssessedStaysEmptyRatherThanBecomingAZero() {
        // Un zéro affirme « niveau nul, constaté ». Une case vide dit « on ne
        // sait pas ». Les confondre ferait accuser des gens qu'on n'a jamais
        // évalués, et fausserait toute lecture de couverture.
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Groupe"),
                        skill(RISQUES, "RISK", "Risques", "Groupe")),
                List.of(assess(ANNA, "Anna", PLANIF, 4)));

        CompetencyGrid.Group groupe = grid.groups().get(0);
        assertThat(groupe.rows().get(0).levels()).containsExactly(4);
        assertThat(groupe.rows().get(1).levels()).containsOnlyNulls();
    }

    @Test
    void theLevelsOfARowFollowTheOrderOfTheColumns() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Groupe")),
                List.of(assess(BORIS, "Boris", PLANIF, 2), assess(ANNA, "Anna", PLANIF, 4)));

        assertThat(grid.people()).extracting(CompetencyGrid.Person::userId)
                .containsExactly(ANNA, BORIS);
        assertThat(grid.groups().get(0).rows().get(0).levels()).containsExactly(4, 2);
    }

    @Test
    void aSkillHeldByASinglePersonIsFlagged() {
        // C'est la lecture qui justifie la figure : une compétence qui ne tient
        // qu'à une personne est un risque d'organisation, pas une donnée.
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Groupe")),
                List.of(assess(ANNA, "Anna", PLANIF, 4), assess(BORIS, "Boris", PLANIF, 0)));

        CompetencyGrid.Row row = grid.groups().get(0).rows().get(0);
        assertThat(row.holders()).isEqualTo(1);
        assertThat(row.singlePointOfKnowledge()).isTrue();
    }

    @Test
    void aSkillNobodyHoldsIsNotASinglePointOfKnowledge() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Groupe")),
                List.of(assess(ANNA, "Anna", PLANIF, 0)));

        CompetencyGrid.Row row = grid.groups().get(0).rows().get(0);
        assertThat(row.holders()).isZero();
        assertThat(row.singlePointOfKnowledge()).isFalse();
    }

    @Test
    void anAssessmentOnAnUnknownSkillIsIgnoredRatherThanCreatingAPhantomRow() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Groupe")),
                List.of(assess(ANNA, "Anna", UUID.randomUUID(), 3)));

        assertThat(grid.groups().get(0).rows()).hasSize(1);
        assertThat(grid.people()).extracting(CompetencyGrid.Person::label).containsExactly("Anna");
    }

    @Test
    void aPersonWithoutALabelIsShownByAShortIdentifierRatherThanNothing() {
        CompetencyGrid grid = CompetencyGrid.assemble(
                List.of(skill(PLANIF, "PLAN", "Planification", "Groupe")),
                List.of(new CompetencyGrid.Assessment(ANNA, null, PLANIF, 3)));

        assertThat(grid.people().get(0).label()).isEqualTo(ANNA.toString().substring(0, 8));
    }

    @Test
    void anEmptyCatalogueYieldsAnEmptyGrid() {
        CompetencyGrid grid = CompetencyGrid.assemble(List.of(), List.of());

        assertThat(grid.groups()).isEmpty();
        assertThat(grid.people()).isEmpty();
    }

    private static CompetencyGrid.SkillEntry skill(UUID id, String code, String name, String category) {
        return new CompetencyGrid.SkillEntry(id, code, name, category);
    }

    private static CompetencyGrid.Assessment assess(UUID userId, String label, UUID skillId, int level) {
        return new CompetencyGrid.Assessment(userId, label, skillId, level);
    }
}
