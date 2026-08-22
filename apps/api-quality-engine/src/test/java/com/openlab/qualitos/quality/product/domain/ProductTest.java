package com.openlab.qualitos.quality.product.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le produit est le sujet du PFMEA et du Control Plan : son code est l'identifiant
 * qu'un auditeur prononcera à voix haute. Il est donc contraint ici, dans le domaine,
 * et pas seulement par une annotation de validation sur un DTO d'entrée — un import
 * ou un appel interne contournerait la seconde, jamais le premier.
 */
class ProductTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-08-19T08:00:00Z");

    @Test
    void aNewProductStartsAsADraft() {
        Product p = Product.create(TENANT, "REF-4471", "Support moteur", USER, NOW);

        assertThat(p.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(p.getCode()).isEqualTo("REF-4471");
        assertThat(p.getTenantId()).isEqualTo(TENANT);
        assertThat(p.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void theCodeIsNormalisedToUpperCaseAndTrimmed() {
        Product p = Product.create(TENANT, "  ref-4471 ", "Support moteur", USER, NOW);

        assertThat(p.getCode()).isEqualTo("REF-4471");
    }

    @Test
    void aBlankCodeIsRefused() {
        assertThatThrownBy(() -> Product.create(TENANT, "   ", "Support moteur", USER, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    @Test
    void aCodeCarryingSeparatorsOrAccentsIsRefused() {
        assertThatThrownBy(() -> Product.create(TENANT, "réf 4471/A", "Support", USER, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBlankDesignationIsRefused() {
        assertThatThrownBy(() -> Product.create(TENANT, "REF-4471", "  ", USER, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("designation");
    }

    @Test
    void anObsoleteProductCannotComeBackToLife() {
        Product p = Product.create(TENANT, "REF-4471", "Support moteur", USER, NOW);
        p.activate();
        p.markObsolete();

        assertThatThrownBy(p::activate)
                .isInstanceOf(ProductStateException.class);
    }

    @Test
    void renamingAnObsoleteProductIsRefused() {
        Product p = Product.create(TENANT, "REF-4471", "Support moteur", USER, NOW);
        p.activate();
        p.markObsolete();

        assertThatThrownBy(() -> p.rename("Autre nom"))
                .isInstanceOf(ProductStateException.class);
    }

    @Test
    void renamingADraftOrActiveProductSucceeds() {
        Product p = Product.create(TENANT, "REF-4471", "Support moteur", USER, NOW);

        p.rename("Support moteur V2");

        assertThat(p.getDesignation()).isEqualTo("Support moteur V2");
    }

    @Test
    void describingAProductSetsAllOptionalFields() {
        Product p = Product.create(TENANT, "REF-4471", "Support moteur", USER, NOW);
        UUID owner = UUID.randomUUID();

        p.describe("Chassis", "B", "Client X", "Site Nord", owner);

        assertThat(p.getFamily()).isEqualTo("Chassis");
        assertThat(p.getRevisionIndex()).isEqualTo("B");
        assertThat(p.getCustomerLabel()).isEqualTo("Client X");
        assertThat(p.getSiteLabel()).isEqualTo("Site Nord");
        assertThat(p.getOwnerUserId()).isEqualTo(owner);
    }

    @Test
    void describingAnObsoleteProductIsRefused() {
        Product p = Product.create(TENANT, "REF-4471", "Support moteur", USER, NOW);
        p.activate();
        p.markObsolete();

        assertThatThrownBy(() -> p.describe("Chassis", "B", "Client X", "Site Nord", USER))
                .isInstanceOf(ProductStateException.class);
    }

    @Test
    void aNullCodeIsRefused() {
        assertThatThrownBy(() -> Product.create(TENANT, null, "Support moteur", USER, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code");
    }

    @Test
    void aDesignationOverTheMaxLengthIsRefused() {
        String tooLong = "A".repeat(251);
        assertThatThrownBy(() -> Product.create(TENANT, "REF-4471", tooLong, USER, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("designation");
    }

    @Test
    void aNullDesignationIsRefused() {
        assertThatThrownBy(() -> Product.create(TENANT, "REF-4471", null, USER, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("designation");
    }

    @Test
    void rehydrate_withoutUpdatedAt_defaultsToCreatedAt() {
        Product p = Product.rehydrate(UUID.randomUUID(), TENANT, "REF-4471", "Support moteur",
                null, null, null, null, null, null, USER, NOW, null);

        assertThat(p.getUpdatedAt()).isEqualTo(NOW);
        assertThat(p.getStatus()).isEqualTo(ProductStatus.DRAFT);
    }
}
