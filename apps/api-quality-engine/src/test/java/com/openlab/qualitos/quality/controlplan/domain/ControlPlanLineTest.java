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

        line.describe(operation, "Tour CN 3", "12", null, "Ø 20 ±0,05",
                new BigDecimal("19.95"), new BigDecimal("20.05"), "mm",
                "Micromètre", 5, "1 pièce / heure", "Carte X-R", "Tri à 100 %");

        assertThat(line.getOperationId()).isEqualTo(operation);
        assertThat(line.getMachine()).isEqualTo("Tour CN 3");
        assertThat(line.getCharacteristicNo()).isEqualTo("12");
        assertThat(line.getSpecialClass()).isEqualTo(CharacteristicClass.STANDARD);
        assertThat(line.getSpecification()).isEqualTo("Ø 20 ±0,05");
        assertThat(line.getToleranceLower()).isEqualByComparingTo("19.95");
        assertThat(line.getToleranceUpper()).isEqualByComparingTo("20.05");
        assertThat(line.getUnit()).isEqualTo("mm");
        assertThat(line.getMeasurementTechnique()).isEqualTo("Micromètre");
        assertThat(line.getSampleSize()).isEqualTo(5);
        assertThat(line.getSampleFrequency()).isEqualTo("1 pièce / heure");
        assertThat(line.getControlMethod()).isEqualTo("Carte X-R");
        assertThat(line.getReactionPlan()).isEqualTo("Tri à 100 %");
    }

    @Test
    void aSafetyCharacteristicKeepsItsClass() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Effort d'arrachement",
                CharacteristicType.PRODUCT);

        line.describe(null, null, null, CharacteristicClass.SAFETY, null, null, null, null,
                null, null, null, null, null);

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
    void assignIdStampsThePersistedIdentity() {
        ControlPlanLine line = ControlPlanLine.create(TENANT, PLAN, 10, "Cote", CharacteristicType.PRODUCT);
        UUID id = UUID.randomUUID();

        line.assignId(id);

        assertThat(line.getId()).isEqualTo(id);
    }
}
