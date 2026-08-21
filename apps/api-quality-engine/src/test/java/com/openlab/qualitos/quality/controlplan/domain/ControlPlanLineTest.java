package com.openlab.qualitos.quality.controlplan.domain;

import com.openlab.qualitos.quality.risk.CharacteristicClass;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La ligne AIAG : ce qu'elle exige, et tout ce qu'elle laisse en suspens.
 *
 * <p>Un plan se remplit par passes — la liste des caractéristiques, puis les
 * moyens de mesure, puis les fréquences. Exiger la ligne complète du premier coup
 * n'améliorerait pas le document, cela empêcherait simplement de commencer.
 */
class ControlPlanLineTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PLAN = UUID.randomUUID();

    @Test
    void onlyTheCharacteristicAndItsTypeAreRequired() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Diamètre alésage",
                CharacteristicType.PRODUCT);

        assertThat(line.getCharacteristicLabel()).isEqualTo("Diamètre alésage");
        assertThat(line.getCharacteristicType()).isEqualTo(CharacteristicType.PRODUCT);
        assertThat(line.getSpecialClass()).isEqualTo(CharacteristicClass.STANDARD);
        assertThat(line.getControlMethod()).isNull();
        assertThat(line.getFmeaItemId()).isNull();
    }

    @Test
    void theLabelIsTrimmedBecauseItIsReadOnAPrintedSheet() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "  Couple de serrage  ",
                CharacteristicType.PROCESS);

        assertThat(line.getCharacteristicLabel()).isEqualTo("Couple de serrage");
    }

    @Test
    void aLabelLongerThanTheColumnIsRefused() {
        String tooLong = "x".repeat(501);

        assertThatThrownBy(() -> ControlPlanLine.create(TENANT, PLAN, 10, tooLong,
                CharacteristicType.PRODUCT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTypeIsAlwaysRequired() {
        assertThatThrownBy(() -> ControlPlanLine.create(TENANT, PLAN, 10, "Cote", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void describeFillsTheAiagColumnsAndDefaultsTheSpecialClass() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote", CharacteristicType.PRODUCT);
        UUID operation = UUID.randomUUID();

        line.describe(new ControlPlanLine.Details(operation, "Tour CN 3", "12", null,
                "Ø 20 ±0,05", new BigDecimal("19.95"), new BigDecimal("20.05"), "mm",
                "Micromètre", "5 au réglage puis 1 sur 50", "1 pièce / heure",
                "Carte X-R", "Tri à 100 %", "SOP-103", InputOutput.OUTPUT,
                "Opérateur de ligne", "Journal qualité"));

        assertThat(line.getOperationId()).isEqualTo(operation);
        assertThat(line.getMachine()).isEqualTo("Tour CN 3");
        assertThat(line.getCharacteristicNo()).isEqualTo("12");
        assertThat(line.getSpecialClass()).isEqualTo(CharacteristicClass.STANDARD);
        assertThat(line.getSpecification()).isEqualTo("Ø 20 ±0,05");
        assertThat(line.getToleranceLower()).isEqualByComparingTo("19.95");
        assertThat(line.getToleranceUpper()).isEqualByComparingTo("20.05");
        assertThat(line.getUnit()).isEqualTo("mm");
        assertThat(line.getMeasurementTechnique()).isEqualTo("Micromètre");
        assertThat(line.getSampleSize()).isEqualTo("5 au réglage puis 1 sur 50");
        assertThat(line.getSampleFrequency()).isEqualTo("1 pièce / heure");
        assertThat(line.getControlMethod()).isEqualTo("Carte X-R");
        assertThat(line.getReactionPlan()).isEqualTo("Tri à 100 %");
    }

    @Test
    void aSafetyCharacteristicKeepsItsClass() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Effort d'arrachement",
                CharacteristicType.PRODUCT);

        line.describe(new ControlPlanLine.Details(null, null, null, CharacteristicClass.SAFETY,
                null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(line.getSpecialClass()).isEqualTo(CharacteristicClass.SAFETY);
    }

    @Test
    void renamingRevalidatesTheLabel() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote", CharacteristicType.PRODUCT);

        line.rename("Rugosité", CharacteristicType.PROCESS, 20);

        assertThat(line.getCharacteristicLabel()).isEqualTo("Rugosité");
        assertThat(line.getCharacteristicType()).isEqualTo(CharacteristicType.PROCESS);
        assertThat(line.getSequenceNo()).isEqualTo(20);
        assertThatThrownBy(() -> line.rename(" ", CharacteristicType.PROCESS, 20))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRehydratedLineCarriesItsIdentity() {
        UUID id = UUID.randomUUID();

        ControlPlanLine line = ControlPlanLine.rehydrate(id, TENANT, PLAN, 30, "Cote",
                CharacteristicType.PRODUCT);
        line.justifiedBy(id);

        assertThat(line.getId()).isEqualTo(id);
        assertThat(line.getPlanId()).isEqualTo(PLAN);
        assertThat(line.getTenantId()).isEqualTo(TENANT);
        assertThat(line.getSequenceNo()).isEqualTo(30);
        assertThat(line.getFmeaItemId()).isEqualTo(id);
    }

    @Test
    void aLineCarriesTheFourColumnsTheAiagFormExpects() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Longueur de coupe",
                CharacteristicType.PRODUCT);

        line.describe(new ControlPlanLine.Details(null, null, null, null, null, null, null, null,
                null, "100 % (automatisé)", "Chaque fil", null, null,
                "SOP-101", InputOutput.OUTPUT, "Opérateur / capteur", "Production"));

        assertThat(line.getSopReference()).isEqualTo("SOP-101");
        assertThat(line.getInputOutput()).isEqualTo(InputOutput.OUTPUT);
        assertThat(line.getWhoMeasures()).isEqualTo("Opérateur / capteur");
        assertThat(line.getRecordingLocation()).isEqualTo("Production");
    }

    /**
     * « 5 pièces au réglage puis 1 sur 50 » est une taille d'échantillon
     * parfaitement valide, et aucune ne tient dans un entier. La colonne était
     * typée nombre : il fallait tronquer la règle, ou l'écrire ailleurs.
     */
    @Test
    void aSampleSizeCanBeARuleAndNotJustANumber() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Dénudage",
                CharacteristicType.PROCESS);

        line.describe(new ControlPlanLine.Details(null, null, null, null, null, null, null, null,
                null, "5 pièces au réglage, puis 1 sur 50", "Au réglage et toutes les 50 pièces",
                null, null, null, null, null, null));

        assertThat(line.getSampleSize()).isEqualTo("5 pièces au réglage, puis 1 sur 50");
    }

    @Test
    void describingWithoutDetailsIsRefusedRatherThanClearingTheLine() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote",
                CharacteristicType.PRODUCT);

        assertThatThrownBy(() -> line.describe(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * La validation de longueur ne vit pas qu'à la frontière HTTP : le moteur de
     * propositions de révision écrit des lignes sans passer par le contrôleur.
     * Sans garde ici, une valeur trop longue atteignait la base et revenait en
     * erreur d'intégrité — un 500 là où l'appelant méritait un refus nommé.
     */
    @Test
    void aFieldLongerThanItsColumnIsRefusedByTheDomainItself() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote",
                CharacteristicType.PRODUCT);
        String trop = "x".repeat(300);

        assertThatThrownBy(() -> line.describe(new ControlPlanLine.Details(null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, trop, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("whoMeasures");
    }

    @Test
    void aSopReferenceLongerThanItsColumnIsRefused() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote",
                CharacteristicType.PRODUCT);

        assertThatThrownBy(() -> line.describe(new ControlPlanLine.Details(null, null, null, null,
                null, null, null, null, null, null, null, null, null, "S".repeat(65), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sopReference");
    }

    @Test
    void aRuleOfSampleSizeLongerThanItsColumnIsRefused() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote",
                CharacteristicType.PRODUCT);

        assertThatThrownBy(() -> line.describe(new ControlPlanLine.Details(null, null, null, null,
                null, null, null, null, null, "5".repeat(121), null, null, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sampleSize");
    }

    @Test
    void assignIdStampsThePersistedIdentity() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote", CharacteristicType.PRODUCT);
        UUID id = UUID.randomUUID();

        line.assignId(id);

        assertThat(line.getId()).isEqualTo(id);
    }
}
