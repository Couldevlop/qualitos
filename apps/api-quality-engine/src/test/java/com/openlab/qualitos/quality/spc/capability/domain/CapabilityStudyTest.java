package com.openlab.qualitos.quality.spc.capability.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CapabilityStudyTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTEUR = UUID.randomUUID();
    private static final Instant T0 = Instant.parse("2026-10-20T08:00:00Z");
    private static final Instant T1 = Instant.parse("2026-10-21T08:00:00Z");

    private static CapabilityStudy etude(CapabilityStudyDetails d) {
        return CapabilityStudy.record(TENANT, d, CapabilityThresholds.DEFAULT, ACTEUR, T0);
    }

    private static CapabilityStudyDetails avec(UnaryOperator<Champs> f) {
        return f.apply(new Champs()).details();
    }

    /** Les champs d'une saisie, à modifier un par un. */
    private static final class Champs {
        String part = "Shaft AX";
        String characteristic = "Weight";
        String specification = "500 ± 25 gram";
        String machine = "M-04";
        UUID leader = UUID.randomUUID();
        String leaderName = "J. Martin";
        LocalDate date = LocalDate.of(2026, 9, 14);
        Double lsl = SpcFixtures.LSL;
        Double usl = SpcFixtures.USL;
        Integer n = 5;
        List<List<Double>> grille = SpcFixtures.goldenGrid();

        CapabilityStudyDetails details() {
            return new CapabilityStudyDetails(part, characteristic, specification, machine, leader, leaderName,
                    date, true, lsl, usl, n, grille);
        }
    }

    @Test
    @DisplayName("chemin doré : l'étude du classeur s'enregistre maîtrisée, Cp 1,840 et Cpk 1,769")
    void cheminDore() {
        CapabilityStudy e = etude(SpcFixtures.goldenDetails());

        assertThat(e.getCp()).isEqualTo(1.840);
        assertThat(e.getCpk()).isEqualTo(1.769);
        assertThat(e.status()).isEqualTo(CapabilityStatus.CAPABLE);
        assertThat(e.getPartReference()).isEqualTo("Shaft AX");
        assertThat(e.getLeaderName()).isEqualTo("J. Martin");
        assertThat(e.getSubgroupSize()).isEqualTo(5);
        assertThat(e.getMeasurements()).hasSize(5);
        assertThat(e.getTenantId()).isEqualTo(TENANT);
        assertThat(e.getCreatedBy()).isEqualTo(ACTEUR);
        assertThat(e.getCreatedAt()).isEqualTo(T0);
        assertThat(e.getUpdatedAt()).isEqualTo(T0);
        assertThat(e.getThresholds()).isEqualTo(CapabilityThresholds.DEFAULT);
        assertThat(e.calculation().cpk()).isEqualTo(e.calculation().cpl());
    }

    @Test
    @DisplayName("les textes sont épurés, les champs facultatifs vides deviennent nuls")
    void textesEpures() {
        CapabilityStudy e = etude(avec(c -> {
            c.part = "  Bride BR-120 ";
            c.characteristic = " ";
            c.specification = null;
            c.machine = "";
            c.leader = null;
            c.leaderName = null;
            return c;
        }));
        assertThat(e.getPartReference()).isEqualTo("Bride BR-120");
        assertThat(e.getCharacteristic()).isNull();
        assertThat(e.getSpecification()).isNull();
        assertThat(e.getMachine()).isNull();
        assertThat(e.getLeaderUserId()).isNull();
        assertThat(e.getLeaderName()).isNull();
    }

    @Test
    @DisplayName("la grille enregistrée est une copie : modifier la saisie ensuite ne la change pas")
    void grilleCopiee() {
        List<List<Double>> grille = SpcFixtures.goldenGrid();
        CapabilityStudy e = etude(SpcFixtures.details(grille, 5));
        grille.get(0).set(0, 999.0);
        assertThat(e.getMeasurements().get(0).get(0)).isEqualTo(504.0);
        assertThatThrownBy(() -> e.getMeasurements().get(0).set(0, 1.0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("corriger recalcule Cp et Cpk mais garde les seuils de la création")
    void corrigerRecalcule() {
        CapabilityThresholds stricts = new CapabilityThresholds(new BigDecimal("2.00"), new BigDecimal("1.50"));
        CapabilityStudy e = CapabilityStudy.record(TENANT, SpcFixtures.goldenDetails(), stricts, ACTEUR, T0);
        assertThat(e.status()).isEqualTo(CapabilityStatus.WATCH);

        e.revise(avec(c -> {
            c.lsl = 490.0;
            c.usl = 510.0;
            return c;
        }), T1);

        assertThat(e.getCp()).isEqualTo(0.736);
        assertThat(e.getCpk()).isEqualTo(0.665);
        assertThat(e.getThresholds()).isEqualTo(stricts);
        assertThat(e.status()).isEqualTo(CapabilityStatus.NOT_CAPABLE);
        assertThat(e.getUpdatedAt()).isEqualTo(T1);
        assertThat(e.getCreatedAt()).isEqualTo(T0);
        assertThat(e.isActionPlan()).isTrue();
    }

    @Test
    void lePlanDActionsSeCocheSansToucherAuCalcul() {
        CapabilityStudy e = etude(SpcFixtures.goldenDetails());
        e.setActionPlan(true, T1);
        assertThat(e.isActionPlan()).isTrue();
        assertThat(e.getUpdatedAt()).isEqualTo(T1);
        assertThat(e.getCpk()).isEqualTo(1.769);
    }

    @Test
    void sansTenantNiSeuilsPasDEtude() {
        assertThatNullPointerException()
                .isThrownBy(() -> CapabilityStudy.record(null, SpcFixtures.goldenDetails(),
                        CapabilityThresholds.DEFAULT, ACTEUR, T0));
        assertThatNullPointerException()
                .isThrownBy(() -> CapabilityStudy.record(TENANT, SpcFixtures.goldenDetails(), null, ACTEUR, T0));
    }

    @Test
    void sansSaisiePasDEtude() {
        refuse(null, "partReference");
    }

    @Test
    void laReferenceDeLaPieceEstObligatoireEtBornee() {
        refuse(avec(c -> { c.part = "  "; return c; }), "partReference");
        refuse(avec(c -> { c.part = null; return c; }), "partReference");
        refuse(avec(c -> { c.part = "x".repeat(121); return c; }), "partReference");
    }

    @Test
    void lesChampsFacultatifsSontBornes() {
        refuse(avec(c -> { c.characteristic = "x".repeat(151); return c; }), "characteristic");
        refuse(avec(c -> { c.specification = "x".repeat(151); return c; }), "specification");
        refuse(avec(c -> { c.machine = "x".repeat(81); return c; }), "machine");
        refuse(avec(c -> { c.leaderName = "x".repeat(151); return c; }), "leaderName");
    }

    @Test
    void unMeneurChoisiDansLAnnuaireAUnNom() {
        refuse(avec(c -> { c.leaderName = " "; return c; }), "leaderName");
    }

    @Test
    void laDateEstObligatoire() {
        refuse(avec(c -> { c.date = null; return c; }), "studyDate");
    }

    @Test
    void lesLimitesSontObligatoiresFiniesEtOrdonnees() {
        refuse(avec(c -> { c.lsl = null; return c; }), "lsl");
        refuse(avec(c -> { c.usl = null; return c; }), "usl");
        refuse(avec(c -> { c.lsl = Double.NaN; return c; }), "lsl");
        refuse(avec(c -> { c.usl = Double.POSITIVE_INFINITY; return c; }), "usl");
        refuse(avec(c -> { c.usl = 2e12; return c; }), "usl");
        refuse(avec(c -> { c.usl = 475.0; return c; }), "usl");
        refuse(avec(c -> { c.usl = 400.0; return c; }), "usl");
    }

    @Test
    void laTailleDuSousGroupeEstObligatoireEtDansLaTable() {
        refuse(avec(c -> { c.n = null; return c; }), "subgroupSize");
        refuse(avec(c -> { c.n = 11; return c; }), "subgroupSize");
    }

    @Test
    void laGrilleAUneLigneParEchantillonEtVingtSousGroupes() {
        refuse(avec(c -> { c.grille = null; return c; }), "measurements");
        refuse(avec(c -> { c.n = 4; return c; }), "measurements");
        refuse(avec(c -> {
            c.grille = SpcFixtures.goldenGrid();
            c.grille.set(1, null);
            return c;
        }), "measurements");
        refuse(avec(c -> {
            c.grille = SpcFixtures.goldenGrid();
            c.grille.set(1, new ArrayList<>(c.grille.get(1).subList(0, 19)));
            return c;
        }), "measurements");
    }

    @Test
    void uneMesureHorsPlageEstRefusee() {
        refuse(avec(c -> { c.grille.get(0).set(3, Double.NaN); return c; }), "measurements");
        refuse(avec(c -> { c.grille.get(0).set(3, -2e12); return c; }), "measurements");
    }

    @Test
    void ilFautAuMoinsUnSousGroupeCompletEtDeLaDispersion() {
        refuse(avec(c -> { c.grille = SpcFixtures.emptyGrid(5); return c; }), "measurements");
        refuse(avec(c -> {
            c.n = 2;
            c.grille = SpcFixtures.emptyGrid(2);
            c.grille.get(0).set(0, 500.0);
            c.grille.get(1).set(0, 500.0);
            return c;
        }), "measurements");
    }

    @Test
    @DisplayName("une correction refusée laisse l'étude intacte")
    void correctionRefusee() {
        CapabilityStudy e = etude(SpcFixtures.goldenDetails());
        assertThatThrownBy(() -> e.revise(avec(c -> { c.usl = 470.0; return c; }), T1))
                .isInstanceOf(SpcValidationException.class);
        assertThat(e.getUsl()).isEqualTo(SpcFixtures.USL);
        assertThat(e.getUpdatedAt()).isEqualTo(T0);
    }

    private static void refuse(CapabilityStudyDetails d, String champ) {
        assertThatThrownBy(() -> etude(d))
                .isInstanceOf(SpcValidationException.class)
                .extracting("field").isEqualTo(champ);
    }
}
