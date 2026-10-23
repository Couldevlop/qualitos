package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.training.CompetencySource;
import com.openlab.qualitos.quality.training.Skill;
import com.openlab.qualitos.quality.training.SkillRepository;
import com.openlab.qualitos.quality.training.UserSkillAssignment;
import com.openlab.qualitos.quality.training.UserSkillAssignmentRepository;
import com.openlab.qualitos.quality.training.competencymatrix.domain.LevelCell;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixOperator;
import com.openlab.qualitos.quality.training.competencymatrix.domain.MatrixSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les adaptateurs sur un vrai mapping JPA (contexte complet, H2 en mode
 * PostgreSQL) : la frontière entre clients, la reprise du catalogue, et le
 * niveau qui EST l'évaluation de compétence.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("web")
class CompetencyMatrixAdaptersTest {

    @Autowired MatrixOperatorJpaRepository operatorsJpa;
    @Autowired MatrixSkillJpaRepository skillsJpa;
    @Autowired CompetencyWorkshopJpaRepository workshopsJpa;
    @Autowired SkillRepository catalogue;
    @Autowired UserSkillAssignmentRepository assessments;
    @Autowired Clock clock;

    CompetencyMatrixAdapters.Operators operators;
    CompetencyMatrixAdapters.Skills skills;
    CompetencyMatrixAdapters.Levels levels;
    CompetencyMatrixAdapters.WorkshopSettings workshops;

    final UUID tenant = UUID.randomUUID();
    final UUID autre = UUID.randomUUID();
    final UUID acteur = UUID.randomUUID();
    final Instant maintenant = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @BeforeEach
    void setup() {
        operators = new CompetencyMatrixAdapters.Operators(operatorsJpa);
        skills = new CompetencyMatrixAdapters.Skills(skillsJpa, catalogue);
        levels = new CompetencyMatrixAdapters.Levels(assessments);
        workshops = new CompetencyMatrixAdapters.WorkshopSettings(workshopsJpa, clock);
    }

    @Test
    void unOperateurSInscritSeRelitEtSeRetireDansSonSeulClient() {
        UUID personne = UUID.randomUUID();
        MatrixOperator o = operators.add(tenant, personne, "Martin Dubois", "Usinage", acteur, maintenant);

        assertThat(operators.findAll(tenant)).containsExactly(o);
        assertThat(operators.findByUser(tenant, personne)).contains(o);
        assertThat(operators.findById(tenant, o.id())).contains(o);
        // Un autre client ne la voit pas, même avec l'identifiant exact, et ne peut pas la retirer.
        assertThat(operators.findAll(autre)).isEmpty();
        assertThat(operators.findById(autre, o.id())).isEmpty();
        assertThat(operators.findByUser(autre, personne)).isEmpty();
        operators.remove(autre, o.id());
        assertThat(operators.findAll(tenant)).hasSize(1);

        operators.remove(tenant, o.id());
        assertThat(operators.findAll(tenant)).isEmpty();
    }

    @Test
    void uneCompetenceCreeeDepuisLaMatriceRecoitUnCodeLibre() {
        UUID premiere = skills.createCatalogueSkill(tenant, "Sécurité / habilitations", "Sécurité");
        UUID seconde = skills.createCatalogueSkill(tenant, "Sécurité  habilitations", "Sécurité");

        assertThat(catalogue.findById(premiere).orElseThrow().getCode()).isEqualTo("securite-habilitations");
        assertThat(catalogue.findById(seconde).orElseThrow().getCode()).isEqualTo("securite-habilitations-2");
        // Le même code reste libre chez un autre client.
        UUID ailleurs = skills.createCatalogueSkill(autre, "Sécurité / habilitations", "Sécurité");
        assertThat(catalogue.findById(ailleurs).orElseThrow().getCode()).isEqualTo("securite-habilitations");
    }

    @Test
    void leCatalogueEstReprisParIntituleSansEgardALaCasseEtDansLeSeulClient() {
        Skill s = new Skill();
        s.setTenantId(tenant);
        s.setCode("tournage-cnc");
        s.setName("Tournage CNC");
        catalogue.save(s);

        assertThat(skills.findCatalogueSkillByName(tenant, "tournage cnc")).contains(s.getId());
        assertThat(skills.findCatalogueSkillByName(autre, "Tournage CNC")).isEmpty();

        skills.categorizeIfMissing(autre, s.getId(), "Pirate");
        assertThat(catalogue.findById(s.getId()).orElseThrow().getCategory()).isNull();
        skills.categorizeIfMissing(tenant, s.getId(), "Usinage");
        skills.categorizeIfMissing(tenant, s.getId(), "Autre");
        assertThat(catalogue.findById(s.getId()).orElseThrow().getCategory()).isEqualTo("Usinage");
    }

    @Test
    void uneCompetenceEnColonneSeRelitAvecSonFormateurDansSonSeulClient() {
        UUID id = skills.createCatalogueSkill(tenant, "Tournage CNC", "Usinage");
        UUID formateur = UUID.randomUUID();
        MatrixSkill m = skills.add(tenant, id, formateur, "Karim Benali", acteur, maintenant);

        assertThat(m.name()).isEqualTo("Tournage CNC");
        assertThat(m.category()).isEqualTo("Usinage");
        assertThat(skills.findAll(tenant)).containsExactly(m);
        assertThat(skills.find(tenant, id)).contains(m);
        assertThat(skills.findAll(autre)).isEmpty();
        assertThat(skills.find(autre, id)).isEmpty();

        skills.remove(autre, id);
        assertThat(skills.findAll(tenant)).hasSize(1);
        skills.remove(tenant, id);
        assertThat(skills.findAll(tenant)).isEmpty();
        // La compétence reste au catalogue.
        assertThat(catalogue.findById(id)).isPresent();
    }

    @Test
    void uneInscriptionQuiDesigneLaCompetenceDUnAutreClientEstIgnoree() {
        UUID etrangere = skills.createCatalogueSkill(autre, "Fraisage", "Usinage");
        MatrixSkillJpaEntity e = new MatrixSkillJpaEntity();
        e.setTenantId(tenant);
        e.setSkillId(etrangere);
        e.setTrainerUserId(UUID.randomUUID());
        e.setTrainerName("X");
        e.setCreatedAt(maintenant);
        skillsJpa.save(e);

        assertThat(skills.findAll(tenant)).isEmpty();
        assertThat(skills.find(tenant, etrangere)).isEmpty();
    }

    @Test
    void poserUnNiveauEcritLEvaluationDeCompetenceEtRendLePrecedent() {
        UUID personne = UUID.randomUUID();
        UUID id = skills.createCatalogueSkill(tenant, "Lecture de plans", "Qualité");
        LocalDate jour = LocalDate.of(2026, 10, 23);

        assertThat(levels.set(tenant, personne, "Paul Girard", id, 1, acteur, jour)).isEqualTo(OptionalInt.empty());
        UserSkillAssignment a = assessments.findByTenantIdAndUserIdAndSkillId(tenant, personne, id).orElseThrow();
        a.setExpiresOn(LocalDate.of(2027, 1, 1));
        assessments.save(a);

        assertThat(levels.set(tenant, personne, "Paul Girard", id, 3, acteur, jour)).isEqualTo(OptionalInt.of(1));

        UserSkillAssignment relue = assessments.findByTenantIdAndUserIdAndSkillId(tenant, personne, id).orElseThrow();
        assertThat(relue.getLevel()).isEqualTo(3);
        assertThat(relue.getUserName()).isEqualTo("Paul Girard");
        assertThat(relue.getSource()).isEqualTo(CompetencySource.MANAGER);
        assertThat(relue.getAssessedBy()).isEqualTo(acteur);
        assertThat(relue.getAssessedAt()).isEqualTo(jour);
        assertThat(relue.getExpiresOn()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(levels.findAll(tenant)).containsExactly(new LevelCell(personne, id, 3));
        assertThat(levels.findAll(autre)).isEmpty();
    }

    @Test
    void lesAteliersSeRemplacentDUnBlocDansLeurOrdre() {
        assertThat(workshops.find(tenant)).isEmpty();

        workshops.replace(tenant, List.of("Usinage", "Peinture"), acteur);
        workshops.replace(tenant, List.of("Peinture", "Usinage", "Montage"), acteur);

        assertThat(workshops.find(tenant)).contains(List.of("Peinture", "Usinage", "Montage"));
        assertThat(workshops.find(autre)).isEmpty();
        assertThat(workshopsJpa.findByTenantIdOrderByPositionAsc(tenant))
                .allSatisfy(w -> assertThat(w.getUpdatedBy()).isEqualTo(acteur));
    }
}
