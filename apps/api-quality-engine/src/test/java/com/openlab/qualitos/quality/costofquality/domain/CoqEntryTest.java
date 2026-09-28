package com.openlab.qualitos.quality.costofquality.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Les règles d'une ligne de coût. Le cœur est la bascule « contrôle de
 * pièces » : quatre champs obligatoires sur ces lignes-là, et seulement sur
 * elles.
 */
class CoqEntryTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTEUR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-09-28T08:00:00Z");
    static final LocalDate SEPT = LocalDate.of(2026, 9, 15);

    static final CoqLabel FORMATION = new CoqLabel(UUID.randomUUID(), null, CoqCategory.PREVENTION,
            "PREVENTION_TRAINING", "Formation qualité du personnel", false, 10);
    static final CoqLabel REBUTS = new CoqLabel(UUID.randomUUID(), null, CoqCategory.INTERNAL_FAILURE,
            "INTERNAL_SCRAP", "Rebuts et mise au rebut", true, 10);

    static CoqEntryDetails simple(String montant) {
        return new CoqEntryDetails(new BigDecimal(montant), "M. Alaoui", SEPT, "  note  ",
                null, null, null, null);
    }

    static CoqEntryDetails pieces(String reference, Integer quantite, String lot, LocalDate date) {
        return new CoqEntryDetails(new BigDecimal("1263.00"), "Mme Diallo", SEPT, null,
                reference, quantite, lot, date);
    }

    @Test
    void uneLigneSimpleSEnregistreEtPrendLaFamilleDeSonLibelle() {
        CoqEntry ligne = CoqEntry.record(TENANT, FORMATION, simple("180"), AUTEUR, NOW);

        assertThat(ligne.getCategory()).isEqualTo(CoqCategory.PREVENTION);
        assertThat(ligne.getLabelId()).isEqualTo(FORMATION.getId());
        assertThat(ligne.getAmount()).isEqualByComparingTo("180");
        assertThat(ligne.getComment()).isEqualTo("note");
        assertThat(ligne.isPartControl()).isFalse();
        assertThat(ligne.getCreatedBy()).isEqualTo(AUTEUR);
        assertThat(ligne.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void lesChampsPiecesSontIgnoresSurUneLigneQuiNeLesDemandePas() {
        CoqEntryDetails avecPieces = new CoqEntryDetails(BigDecimal.TEN, "M. Alaoui", SEPT, null,
                "REF-1", 4, "L-22", SEPT);

        CoqEntry ligne = CoqEntry.record(TENANT, FORMATION, avecPieces, AUTEUR, NOW);

        assertThat(ligne.getPartReference()).isNull();
        assertThat(ligne.getPartQuantity()).isNull();
        assertThat(ligne.getLot()).isNull();
        assertThat(ligne.getReceivedOrMadeOn()).isNull();
    }

    @Test
    void uneLigneDePiecesCompleteGardeSesQuatreChamps() {
        CoqEntry ligne = CoqEntry.record(TENANT, REBUTS,
                pieces(" P-4410 ", 12, " L-2609 ", LocalDate.of(2026, 9, 2)), AUTEUR, NOW);

        assertThat(ligne.isPartControl()).isTrue();
        assertThat(ligne.getPartReference()).isEqualTo("P-4410");
        assertThat(ligne.getPartQuantity()).isEqualTo(12);
        assertThat(ligne.getLot()).isEqualTo("L-2609");
        assertThat(ligne.getReceivedOrMadeOn()).isEqualTo(LocalDate.of(2026, 9, 2));
    }

    @Test
    void uneLigneDePiecesSansReferenceEstRefusee() {
        assertChamp(() -> CoqEntry.record(TENANT, REBUTS, pieces("  ", 3, "L", SEPT), AUTEUR, NOW),
                "partReference");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -2})
    void uneLigneDePiecesSansNombreValideEstRefusee(int quantite) {
        assertChamp(() -> CoqEntry.record(TENANT, REBUTS, pieces("P", quantite, "L", SEPT), AUTEUR, NOW),
                "partQuantity");
        assertChamp(() -> CoqEntry.record(TENANT, REBUTS, pieces("P", null, "L", SEPT), AUTEUR, NOW),
                "partQuantity");
    }

    @Test
    void uneLigneDePiecesSansLotEstRefusee() {
        assertChamp(() -> CoqEntry.record(TENANT, REBUTS, pieces("P", 1, null, SEPT), AUTEUR, NOW), "lot");
    }

    @Test
    void uneLigneDePiecesSansDateDeReceptionEstRefusee() {
        assertChamp(() -> CoqEntry.record(TENANT, REBUTS, pieces("P", 1, "L", null), AUTEUR, NOW),
                "receivedOrMadeOn");
    }

    @Test
    void leResponsableEtLaDateSontObligatoiresPartout() {
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION,
                new CoqEntryDetails(BigDecimal.ONE, " ", SEPT, null, null, null, null, null), AUTEUR, NOW),
                "responsible");
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION,
                new CoqEntryDetails(BigDecimal.ONE, "X", null, null, null, null, null, null), AUTEUR, NOW),
                "imputationDate");
    }

    @Test
    void leMontantEstObligatoirePositifEtADeuxDecimalesAuPlus() {
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION,
                new CoqEntryDetails(null, "X", SEPT, null, null, null, null, null), AUTEUR, NOW), "amount");
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION, simple("-1"), AUTEUR, NOW), "amount");
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION, simple("1.005"), AUTEUR, NOW), "amount");
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION, simple("1000000000000"), AUTEUR, NOW), "amount");
        assertThat(CoqEntry.record(TENANT, FORMATION, simple("0"), AUTEUR, NOW).getAmount())
                .isEqualByComparingTo("0");
    }

    @Test
    void lesTextesTropLongsSontRefuses() {
        String long151 = "x".repeat(151);
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION,
                new CoqEntryDetails(BigDecimal.ONE, long151, SEPT, null, null, null, null, null), AUTEUR, NOW),
                "responsible");
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION,
                new CoqEntryDetails(BigDecimal.ONE, "X", SEPT, "x".repeat(2001), null, null, null, null),
                AUTEUR, NOW), "comment");
    }

    @Test
    void sansLibelleNiDetailRienNeSEnregistre() {
        assertChamp(() -> CoqEntry.record(TENANT, null, simple("1"), AUTEUR, NOW), "labelId");
        assertChamp(() -> CoqEntry.record(TENANT, FORMATION, null, AUTEUR, NOW), "amount");
    }

    @Test
    void corrigerVersUnLibelleSansPiecesEffaceLesChampsPieces() {
        CoqEntry ligne = CoqEntry.record(TENANT, REBUTS, pieces("P", 2, "L", SEPT), AUTEUR, NOW);
        Instant plusTard = NOW.plusSeconds(60);

        ligne.revise(FORMATION, simple("50"), plusTard);

        assertThat(ligne.getCategory()).isEqualTo(CoqCategory.PREVENTION);
        assertThat(ligne.isPartControl()).isFalse();
        assertThat(ligne.getLot()).isNull();
        assertThat(ligne.getUpdatedAt()).isEqualTo(plusTard);
        assertThat(ligne.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void uneCorrectionRefuseeLaisseLaLigneIntacte() {
        CoqEntry ligne = CoqEntry.record(TENANT, FORMATION, simple("50"), AUTEUR, NOW);

        assertThatThrownBy(() -> ligne.revise(REBUTS, simple("80"), NOW))
                .isInstanceOf(CoqValidationException.class);

        assertThat(ligne.getAmount()).isEqualByComparingTo("50");
        assertThat(ligne.getCategory()).isEqualTo(CoqCategory.PREVENTION);
    }

    private static void assertChamp(org.assertj.core.api.ThrowableAssert.ThrowingCallable geste, String champ) {
        assertThatThrownBy(geste)
                .isInstanceOf(CoqValidationException.class)
                .extracting(e -> ((CoqValidationException) e).getField())
                .isEqualTo(champ);
    }
}
