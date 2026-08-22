package com.openlab.qualitos.quality.product.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ligne de nomenclature. Le fournisseur est optionnel — une pièce peut être fabriquée
 * en interne — donc seule la référence est un invariant du domaine.
 */
class ProductComponentTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID PRODUCT = UUID.randomUUID();
    static final UUID SUPPLIER = UUID.randomUUID();

    @Test
    void aFullyPopulatedComponentExposesEveryField() {
        ProductComponent c = new ProductComponent(null, TENANT, PRODUCT, 1,
                "BOLT-M6", "Vis M6", new BigDecimal("4.0000"), "pcs", SUPPLIER);

        assertThat(c.getId()).isNull();
        assertThat(c.getTenantId()).isEqualTo(TENANT);
        assertThat(c.getProductId()).isEqualTo(PRODUCT);
        assertThat(c.getSequenceNo()).isEqualTo(1);
        assertThat(c.getReference()).isEqualTo("BOLT-M6");
        assertThat(c.getLabel()).isEqualTo("Vis M6");
        assertThat(c.getQuantity()).isEqualByComparingTo("4.0000");
        assertThat(c.getUnit()).isEqualTo("pcs");
        assertThat(c.getSupplierId()).isEqualTo(SUPPLIER);
    }

    @Test
    void theSupplierIsOptional_madeInHouseComponent() {
        ProductComponent c = new ProductComponent(null, TENANT, PRODUCT, 2,
                "PLATE-01", "Plaque usinée", BigDecimal.ONE, "pcs", null);

        assertThat(c.getSupplierId()).isNull();
    }

    @Test
    void aBlankReferenceIsRefused() {
        assertThatThrownBy(() -> new ProductComponent(null, TENANT, PRODUCT, 1,
                "   ", "Vis M6", BigDecimal.ONE, "pcs", SUPPLIER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reference");
    }

    @Test
    void aNullReferenceIsRefused() {
        assertThatThrownBy(() -> new ProductComponent(null, TENANT, PRODUCT, 1,
                null, "Vis M6", BigDecimal.ONE, "pcs", SUPPLIER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reference");
    }

    @Test
    void aReferenceOverTheMaxLengthIsRefused() {
        String tooLong = "A".repeat(121);
        assertThatThrownBy(() -> new ProductComponent(null, TENANT, PRODUCT, 1,
                tooLong, "Vis M6", BigDecimal.ONE, "pcs", SUPPLIER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMissingTenantIsRefused() {
        assertThatThrownBy(() -> new ProductComponent(null, null, PRODUCT, 1,
                "BOLT-M6", "Vis M6", BigDecimal.ONE, "pcs", SUPPLIER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aMissingProductIsRefused() {
        assertThatThrownBy(() -> new ProductComponent(null, TENANT, null, 1,
                "BOLT-M6", "Vis M6", BigDecimal.ONE, "pcs", SUPPLIER))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void assigningAnIdMakesItVisibleThroughTheGetter() {
        ProductComponent c = new ProductComponent(null, TENANT, PRODUCT, 1,
                "BOLT-M6", "Vis M6", BigDecimal.ONE, "pcs", SUPPLIER);
        UUID id = UUID.randomUUID();

        c.assignId(id);

        assertThat(c.getId()).isEqualTo(id);
    }
}
