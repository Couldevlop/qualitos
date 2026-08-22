package com.openlab.qualitos.quality.product.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opération de la gamme : c'est le langage commun entre le PFMEA et le Control Plan
 * (task 3 et 4), donc son code et son libellé sont des invariants du domaine.
 */
class ProductOperationTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();

    @Test
    void aFullyPopulatedOperationExposesEveryField() {
        ProductOperation o = new ProductOperation(null, TENANT, PRODUCT, 10,
                "OP-010", "Perçage", "Poste 3");

        assertThat(o.getId()).isNull();
        assertThat(o.getTenantId()).isEqualTo(TENANT);
        assertThat(o.getProductId()).isEqualTo(PRODUCT);
        assertThat(o.getSequenceNo()).isEqualTo(10);
        assertThat(o.getCode()).isEqualTo("OP-010");
        assertThat(o.getLabel()).isEqualTo("Perçage");
        assertThat(o.getWorkstation()).isEqualTo("Poste 3");
    }

    @Test
    void theWorkstationIsOptional() {
        ProductOperation o = new ProductOperation(null, TENANT, PRODUCT, 10,
                "OP-010", "Perçage", null);

        assertThat(o.getWorkstation()).isNull();
    }

    @Test
    void aBlankCodeIsRefused() {
        assertThatThrownBy(() -> new ProductOperation(null, TENANT, PRODUCT, 10,
                "  ", "Perçage", "Poste 3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    @Test
    void aNullCodeIsRefused() {
        assertThatThrownBy(() -> new ProductOperation(null, TENANT, PRODUCT, 10,
                null, "Perçage", "Poste 3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    @Test
    void aCodeOverTheMaxLengthIsRefused() {
        String tooLong = "A".repeat(33);
        assertThatThrownBy(() -> new ProductOperation(null, TENANT, PRODUCT, 10,
                tooLong, "Perçage", "Poste 3"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBlankLabelIsRefused() {
        assertThatThrownBy(() -> new ProductOperation(null, TENANT, PRODUCT, 10,
                "OP-010", "   ", "Poste 3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label");
    }

    @Test
    void aNullLabelIsRefused() {
        assertThatThrownBy(() -> new ProductOperation(null, TENANT, PRODUCT, 10,
                "OP-010", null, "Poste 3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("label");
    }

    @Test
    void aLabelOverTheMaxLengthIsRefused() {
        String tooLong = "A".repeat(251);
        assertThatThrownBy(() -> new ProductOperation(null, TENANT, PRODUCT, 10,
                "OP-010", tooLong, "Poste 3"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMissingTenantIsRefused() {
        assertThatThrownBy(() -> new ProductOperation(null, null, PRODUCT, 10,
                "OP-010", "Perçage", "Poste 3"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aMissingProductIsRefused() {
        assertThatThrownBy(() -> new ProductOperation(null, TENANT, null, 10,
                "OP-010", "Perçage", "Poste 3"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void assigningAnIdMakesItVisibleThroughTheGetter() {
        ProductOperation o = new ProductOperation(null, TENANT, PRODUCT, 10,
                "OP-010", "Perçage", "Poste 3");
        UUID id = UUID.randomUUID();

        o.assignId(id);

        assertThat(o.getId()).isEqualTo(id);
    }
}
