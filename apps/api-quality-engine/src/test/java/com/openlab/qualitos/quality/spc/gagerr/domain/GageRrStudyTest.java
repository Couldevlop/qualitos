package com.openlab.qualitos.quality.spc.gagerr.domain;

import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** L'étude R&amp;R : validation champ par champ, calcul serveur, verdict enregistré. */
class GageRrStudyTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTEUR = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-10-20T08:00:00Z");

    static GageRrStudy enregistrer(GageRrStudyDetails d) {
        return GageRrStudy.record(TENANT, d, AUTEUR, T0);
    }

    /** Les détails du classeur, retouchés par {@code f}. */
    static GageRrStudyDetails avec(UnaryOperator<Builder> f) {
        return f.apply(new Builder()).build();
    }

    @Test
    @DisplayName("la grille du classeur : 21,00 % de la variation totale, ndc 6, sous conditions")
    void cheminDore() {
        GageRrStudy e = enregistrer(GageRrFixtures.workbookDetails());
        assertThat(e.getGrrPercent()).isEqualTo(21.0);
        assertThat(e.getGrrTolerancePercent()).isEqualTo(24.74);
        assertThat(e.getNdc()).isEqualTo(6);
        assertThat(e.getDecision()).isEqualTo(GageRrDecision.CONDITIONAL);
        assertThat(e.getAnovaGrrPercent()).isEqualTo(21.16);
        assertThat(e.getAnovaNdc()).isEqualTo(6);
        assertThat(e.getOperatorCount()).isEqualTo(3);
        assertThat(e.getPartCount()).isEqualTo(10);
        assertThat(e.getTrialCount()).isEqualTo(3);
        assertThat(e.getSigmaMultiplier()).isEqualTo(5.15);
        assertThat(e.getPartReference()).isEqualTo("Arbre AX-210");
        assertThat(e.getCharacteristic()).isEqualTo("Diamètre extérieur (mm)");
        assertThat(e.getInstrument()).startsWith("Micromètre");
        assertThat(e.getResponsibleName()).isEqualTo("Jeanne Leroy");
        assertThat(e.getResponsibleUserId()).isEqualTo(GageRrFixtures.RESPONSABLE);
        assertThat(e.getOperators()).extracting(GageRrOperator::name)
                .containsExactly("Alice Martin", "Bruno Petit", "Chloé Durand");
        assertThat(e.getMeasurements()).hasSize(3);
        assertThat(e.getStudyDate()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(e.getTenantId()).isEqualTo(TENANT);
        assertThat(e.getCreatedBy()).isEqualTo(AUTEUR);
        assertThat(e.getCreatedAt()).isEqualTo(T0);
        assertThat(e.getUpdatedAt()).isEqualTo(T0);
        assertThat(e.getId()).isNull();
        assertThat(e.getLsl()).isEqualTo(9.9);
        assertThat(e.getUsl()).isEqualTo(10.1);
        assertThat(e.calculation().ndc()).isEqualTo(6);
    }

    @Test
    @DisplayName("la grille enregistrée est une copie : la modifier dehors ne change pas l'étude")
    void copieDefensive() {
        List<List<List<Double>>> grille = GageRrFixtures.workbookGrid();
        GageRrStudy e = enregistrer(GageRrFixtures.details(grille, GageRrFixtures.operators(), 5.15));
        grille.get(0).get(0).set(0, 99.0);
        assertThat(e.getMeasurements().get(0).get(0).get(0)).isEqualTo(9.955);
        assertThatThrownBy(() -> e.getMeasurements().get(0).get(0).set(0, 1.0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("corriger recalcule tout ; le texte est épuré")
    void corriger() {
        GageRrStudy e = enregistrer(GageRrFixtures.workbookDetails());
        Instant t1 = T0.plusSeconds(60);
        e.revise(avec(b -> b.multiplier(6.0).characteristic("  ").instrument(null)
                .partReference("  Arbre AX-211  ")), t1);
        assertThat(e.getSigmaMultiplier()).isEqualTo(6.0);
        assertThat(e.getGrrPercent()).isEqualTo(21.0);
        assertThat(e.getGrrTolerancePercent()).isEqualTo(28.83);
        assertThat(e.getCharacteristic()).isNull();
        assertThat(e.getInstrument()).isNull();
        assertThat(e.getPartReference()).isEqualTo("Arbre AX-211");
        assertThat(e.getUpdatedAt()).isEqualTo(t1);
        assertThat(e.getCreatedAt()).isEqualTo(T0);
    }

    @Test
    void sansResponsableLEtudePasse() {
        GageRrStudy e = enregistrer(avec(b -> b.responsible(null, null)));
        assertThat(e.getResponsibleUserId()).isNull();
        assertThat(e.getResponsibleName()).isNull();
    }

    @Test
    void lesTextesSontObligatoiresOuBornes() {
        refuse(null, "partReference");
        refuse(avec(b -> b.partReference(" ")), "partReference");
        refuse(avec(b -> b.partReference("x".repeat(121))), "partReference");
        refuse(avec(b -> b.characteristic("x".repeat(151))), "characteristic");
        refuse(avec(b -> b.instrument("x".repeat(151))), "instrument");
        refuse(avec(b -> b.responsible(GageRrFixtures.RESPONSABLE, " ")), "responsibleName");
        refuse(avec(b -> b.responsible(null, "Texte libre")), "responsibleUserId");
        refuse(avec(b -> b.responsible(GageRrFixtures.RESPONSABLE, "x".repeat(151))), "responsibleName");
        refuse(avec(b -> b.studyDate(null)), "studyDate");
    }

    @Test
    void lesLimitesSontObligatoiresFiniesEtOrdonnees() {
        refuse(avec(b -> b.limits(null, 10.1)), "lsl");
        refuse(avec(b -> b.limits(9.9, null)), "usl");
        refuse(avec(b -> b.limits(Double.NaN, 10.1)), "lsl");
        refuse(avec(b -> b.limits(9.9, 2e12)), "usl");
        refuse(avec(b -> b.limits(10.1, 9.9)), "usl");
        refuse(avec(b -> b.limits(10.0, 10.0)), "usl");
    }

    @Test
    void lePlanEstDansLaTable() {
        refuse(avec(b -> b.counts(null, 10, 3)), "operatorCount");
        refuse(avec(b -> b.counts(3, null, 3)), "partCount");
        refuse(avec(b -> b.counts(3, 10, null)), "trialCount");
        refuse(avec(b -> b.counts(4, 10, 3)), "operatorCount");
        refuse(avec(b -> b.counts(3, 11, 3)), "partCount");
        refuse(avec(b -> b.counts(3, 10, 4)), "trialCount");
        refuse(avec(b -> b.multiplier(null)), "sigmaMultiplier");
        refuse(avec(b -> b.multiplier(4.0)), "sigmaMultiplier");
    }

    @Test
    void lesOperateursSontDesPersonnesDistinctesDeLAnnuaire() {
        refuse(avec(b -> b.operators(null)), "operators");
        refuse(avec(b -> b.operators(GageRrFixtures.operators().subList(0, 2))), "operators");
        List<GageRrOperator> sansCompte = GageRrFixtures.operators();
        sansCompte.set(1, new GageRrOperator(null, "Bruno Petit"));
        refuse(avec(b -> b.operators(sansCompte)), "operators");
        List<GageRrOperator> avecNul = GageRrFixtures.operators();
        avecNul.set(2, null);
        refuse(avec(b -> b.operators(avecNul)), "operators");
        List<GageRrOperator> sansNom = GageRrFixtures.operators();
        sansNom.set(0, new GageRrOperator(GageRrFixtures.OPERATEUR_A, " "));
        refuse(avec(b -> b.operators(sansNom)), "operators");
        List<GageRrOperator> doublon = GageRrFixtures.operators();
        doublon.set(2, new GageRrOperator(GageRrFixtures.OPERATEUR_A, "Alice Martin"));
        refuse(avec(b -> b.operators(doublon)), "operators");
    }

    @Test
    void laGrilleSuitLePlanEtToutesSesCellulesSontRenseignees() {
        refuse(avec(b -> b.grid(null)), "measurements");
        List<List<List<Double>>> deuxBlocs = GageRrFixtures.workbookGrid();
        deuxBlocs.remove(2);
        refuse(avec(b -> b.grid(deuxBlocs)), "measurements");
        List<List<List<Double>>> blocNul = GageRrFixtures.workbookGrid();
        blocNul.set(1, null);
        refuse(avec(b -> b.grid(blocNul)), "measurements");
        List<List<List<Double>>> pieceEnMoins = GageRrFixtures.workbookGrid();
        pieceEnMoins.get(0).remove(9);
        refuse(avec(b -> b.grid(pieceEnMoins)), "measurements");
        List<List<List<Double>>> ligneNulle = GageRrFixtures.workbookGrid();
        ligneNulle.get(0).set(3, null);
        refuse(avec(b -> b.grid(ligneNulle)), "measurements");
        List<List<List<Double>>> essaiEnMoins = GageRrFixtures.workbookGrid();
        essaiEnMoins.get(1).get(4).remove(2);
        refuse(avec(b -> b.grid(essaiEnMoins)), "measurements");
        List<List<List<Double>>> trou = GageRrFixtures.workbookGrid();
        trou.get(2).get(9).set(1, null);
        refuse(avec(b -> b.grid(trou)), "measurements");
        List<List<List<Double>>> infinie = GageRrFixtures.workbookGrid();
        infinie.get(2).get(9).set(1, Double.POSITIVE_INFINITY);
        refuse(avec(b -> b.grid(infinie)), "measurements");
        List<List<List<Double>>> enorme = GageRrFixtures.workbookGrid();
        enorme.get(2).get(9).set(1, 2e12);
        refuse(avec(b -> b.grid(enorme)), "measurements");
    }

    @Test
    void sansDispersionLEtudeEstRefusee() {
        List<List<List<Double>>> plate = new ArrayList<>();
        for (int o = 0; o < 3; o++) {
            List<List<Double>> bloc = new ArrayList<>();
            for (int p = 0; p < 10; p++) {
                bloc.add(new ArrayList<>(Collections.nCopies(3, 10.0)));
            }
            plate.add(bloc);
        }
        refuse(avec(b -> b.grid(plate)), "measurements");
    }

    private static void refuse(GageRrStudyDetails d, String champ) {
        assertThatThrownBy(() -> enregistrer(d))
                .isInstanceOf(SpcValidationException.class)
                .extracting("field").isEqualTo(champ);
    }

    /** Les détails du classeur, champ par champ remplaçables. */
    static final class Builder {
        String partReference = "Arbre AX-210";
        String characteristic = "Diamètre extérieur (mm)";
        String instrument = "Micromètre";
        Double lsl = GageRrFixtures.LSL;
        Double usl = GageRrFixtures.USL;
        Integer k = 3;
        Integer n = 10;
        Integer r = 3;
        Double multiplier = 5.15;
        LocalDate date = LocalDate.of(2026, 10, 19);
        UUID responsibleId = GageRrFixtures.RESPONSABLE;
        String responsibleName = "Jeanne Leroy";
        List<GageRrOperator> operators = GageRrFixtures.operators();
        List<List<List<Double>>> grid = GageRrFixtures.workbookGrid();

        Builder partReference(String v) { partReference = v; return this; }
        Builder characteristic(String v) { characteristic = v; return this; }
        Builder instrument(String v) { instrument = v; return this; }
        Builder limits(Double l, Double u) { lsl = l; usl = u; return this; }
        Builder counts(Integer kk, Integer nn, Integer rr) { k = kk; n = nn; r = rr; return this; }
        Builder multiplier(Double v) { multiplier = v; return this; }
        Builder studyDate(LocalDate v) { date = v; return this; }
        Builder responsible(UUID id, String name) { responsibleId = id; responsibleName = name; return this; }
        Builder operators(List<GageRrOperator> v) { operators = v; return this; }
        Builder grid(List<List<List<Double>>> v) { grid = v; return this; }

        GageRrStudyDetails build() {
            return new GageRrStudyDetails(partReference, characteristic, instrument, lsl, usl, k, n, r, multiplier,
                    date, responsibleId, responsibleName, operators, grid);
        }
    }
}
