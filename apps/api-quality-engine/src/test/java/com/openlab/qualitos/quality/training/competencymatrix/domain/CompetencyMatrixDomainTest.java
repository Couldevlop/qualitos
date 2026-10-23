package com.openlab.qualitos.quality.training.competencymatrix.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** L'échelle, les ateliers, le code catalogue et l'assemblage de la matrice. */
class CompetencyMatrixDomainTest {

    // ---------------------------------------------------------------- échelle

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void lEchelleVaDeNonFormeAFormateur(int niveau) {
        assertThat(SkillLevel.require(niveau)).isEqualTo(niveau);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 5, 42})
    void unNiveauHorsEchelleEstRefuseEnNommantLeChamp(int niveau) {
        assertThatThrownBy(() -> SkillLevel.require(niveau))
                .isInstanceOf(CompetencyMatrixValidationException.class)
                .extracting(e -> ((CompetencyMatrixValidationException) e).getField()).isEqualTo("level");
    }

    @Test
    void autonomeApartirDuNiveauDeux() {
        assertThat(SkillLevel.isAutonomous(1)).isFalse();
        assertThat(SkillLevel.isAutonomous(2)).isTrue();
        assertThat(SkillLevel.isAutonomous(4)).isTrue();
    }

    // ---------------------------------------------------------------- ateliers

    @Test
    void sansReglageCeSontLesAteliersDeLaPlateforme() {
        Workshops w = Workshops.platformDefaults();
        assertThat(w.names()).containsExactly("Usinage", "Assemblage", "Qualité", "Maintenance");
        assertThat(w.defaults()).isTrue();
    }

    @Test
    void uneListeRegleeEstNettoyeeEtGardeSonOrdre() {
        Workshops w = Workshops.configured(List.of("  Peinture ", "Montage"));
        assertThat(w.names()).containsExactly("Peinture", "Montage");
        assertThat(w.defaults()).isFalse();
        assertThat(w.rank("Montage")).isEqualTo(1);
        assertThat(w.rank("Disparu")).isEqualTo(2);
    }

    @Test
    void unAtelierSeRetrouveSansEgardALaCasse() {
        Workshops w = Workshops.platformDefaults();
        assertThat(w.canonical(" qualité ")).contains("Qualité");
        assertThat(w.canonical("Peinture")).isEmpty();
        assertThat(w.canonical(null)).isEmpty();
    }

    @Test
    void uneListeInvalideEstRefusee() {
        List<String> trop = new ArrayList<>();
        for (int i = 0; i <= Workshops.MAX_COUNT; i++) {
            trop.add("A" + i);
        }
        List<String> avecNul = new ArrayList<>();
        avecNul.add(null);
        for (List<String> liste : List.of(List.<String>of(), trop, List.of("A", " "), avecNul,
                List.of("x".repeat(Workshops.MAX_LENGTH + 1)), List.of("Usinage", "USINAGE"))) {
            assertThatThrownBy(() -> Workshops.configured(liste))
                    .isInstanceOf(CompetencyMatrixValidationException.class)
                    .extracting(e -> ((CompetencyMatrixValidationException) e).getField()).isEqualTo("names");
        }
        assertThatThrownBy(() -> Workshops.configured(null)).isInstanceOf(CompetencyMatrixValidationException.class);
    }

    // ---------------------------------------------------------------- code catalogue

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Sécurité / habilitations|securite-habilitations",
            "Contrôle par ressuage (CND)|controle-par-ressuage-cnd",
            "  Tournage CNC  |tournage-cnc",
            "旋盤|competence",
            "a|competence"})
    void leCodeSeTireDeLIntitule(String nom, String code) {
        assertThat(SkillCode.from(nom)).isEqualTo(code);
    }

    @Test
    void leCodeResteDansLesBornesDuCatalogue() {
        String code = SkillCode.from("mot ".repeat(60));
        assertThat(code.length()).isLessThanOrEqualTo(SkillCode.MAX_BASE);
        assertThat(code).matches("^[a-z0-9][a-z0-9_-]{1,99}$").doesNotEndWith("-");
        assertThat(SkillCode.from(null)).isEqualTo("competence");
        assertThat(SkillCode.candidate("tournage", 1)).isEqualTo("tournage");
        assertThat(SkillCode.candidate("tournage", 3)).isEqualTo("tournage-3");
    }

    // ---------------------------------------------------------------- matrice

    static final UUID MARTIN = UUID.randomUUID();
    static final UUID SOPHIE = UUID.randomUUID();
    static final UUID PAUL = UUID.randomUUID();
    static final UUID TOURNAGE = UUID.randomUUID();
    static final UUID SOUDURE = UUID.randomUUID();
    static final UUID CND = UUID.randomUUID();

    static MatrixOperator op(UUID user, String nom, String atelier) {
        return new MatrixOperator(UUID.randomUUID(), user, nom, atelier, Instant.EPOCH);
    }

    static MatrixSkill sk(UUID id, String nom, String categorie) {
        return new MatrixSkill(id, nom, categorie, UUID.randomUUID(), "Formateur", Instant.EPOCH);
    }

    static CompetencyMatrix matrice() {
        List<MatrixOperator> ops = new ArrayList<>(List.of(
                op(PAUL, "Paul Girard", "Qualité"),
                op(SOPHIE, "Sophie Lambert", "Usinage"),
                op(MARTIN, "Martin Dubois", "Usinage")));
        List<MatrixSkill> sks = new ArrayList<>(List.of(
                sk(CND, "Contrôle par ressuage", "Qualité"),
                sk(SOUDURE, "Soudure TIG", null),
                sk(TOURNAGE, "Tournage CNC", "Usinage")));
        Collections.reverse(sks);
        List<LevelCell> niveaux = List.of(
                new LevelCell(MARTIN, TOURNAGE, 3),
                new LevelCell(SOPHIE, TOURNAGE, 2),
                new LevelCell(PAUL, CND, 1),
                new LevelCell(MARTIN, CND, 2),
                new LevelCell(UUID.randomUUID(), TOURNAGE, 4),
                new LevelCell(MARTIN, UUID.randomUUID(), 4));
        return CompetencyMatrix.assemble(Workshops.platformDefaults(), ops, sks, niveaux);
    }

    @Test
    void lesLignesSeRangentParAtelierPuisParNom() {
        assertThat(matrice().operators()).extracting(MatrixOperator::name)
                .containsExactly("Martin Dubois", "Sophie Lambert", "Paul Girard");
    }

    @Test
    void lesColonnesSeRangentParCategoriePuisParIntitule_lesSansCategorieEnDernier() {
        assertThat(matrice().skills()).extracting(MatrixSkill::name)
                .containsExactly("Contrôle par ressuage", "Tournage CNC", "Soudure TIG");
    }

    @Test
    void unNiveauHorsMatriceEstEcarte() {
        CompetencyMatrix m = matrice();
        assertThat(m.cells()).hasSize(4);
        assertThat(m.level(MARTIN, TOURNAGE)).isEqualTo(3);
        assertThat(m.level(PAUL, SOUDURE)).isZero();
    }

    @Test
    void laCouvertureCompteLesAutonomesEtSignaleLeRisque() {
        List<CompetencyMatrix.Coverage> c = matrice().coverage();
        assertThat(c).extracting(CompetencyMatrix.Coverage::skillId).containsExactly(CND, TOURNAGE, SOUDURE);

        CompetencyMatrix.Coverage cnd = c.get(0);
        assertThat(cnd.autonomous()).isEqualTo(1);
        assertThat(cnd.autonomousNames()).containsExactly("Martin Dubois");
        assertThat(cnd.atRisk()).isTrue();

        CompetencyMatrix.Coverage tournage = c.get(1);
        assertThat(tournage.autonomous()).isEqualTo(2);
        assertThat(tournage.autonomousNames()).containsExactly("Martin Dubois", "Sophie Lambert");
        assertThat(tournage.atRisk()).isFalse();

        CompetencyMatrix.Coverage soudure = c.get(2);
        assertThat(soudure.autonomous()).isZero();
        assertThat(soudure.atRisk()).isTrue();
    }

    @Test
    void unAtelierRetireRangeSesOperateursApresLesAutres() {
        CompetencyMatrix m = CompetencyMatrix.assemble(Workshops.configured(List.of("Qualité")),
                List.of(op(MARTIN, "Martin", "Usinage"), op(PAUL, "Paul", "Qualité"), op(SOPHIE, "Sophie", "Assemblage")),
                List.of(), List.of());
        assertThat(m.operators()).extracting(MatrixOperator::name).containsExactly("Paul", "Sophie", "Martin");
    }
}
