package com.openlab.qualitos.quality.tenantmodules.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleCatalogTest {

    @Test
    void all_isImmutableAndNonEmpty() {
        assertThat(ModuleCatalog.all()).isNotEmpty();
        assertThatThrownBy(() -> ModuleCatalog.all().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void coreModules_areFlaggedAsCore() {
        assertThat(ModuleCatalog.require("pdca").coreModule()).isTrue();
        assertThat(ModuleCatalog.require("capa").coreModule()).isTrue();
        assertThat(ModuleCatalog.require("audit").coreModule()).isTrue();
    }

    @Test
    void enterpriseTier_isAssignedToBlockchain() {
        assertThat(ModuleCatalog.require("blockchain").minimumTier())
                .isEqualTo(BillingTier.ENTERPRISE);
    }

    @Test
    void dependencies_areTransitivelyKnown() {
        // Toutes les dépendances déclarées doivent référencer un module existant.
        for (ModuleCatalogEntry e : ModuleCatalog.all()) {
            for (String dep : e.dependencies()) {
                assertThat(ModuleCatalog.contains(dep))
                        .as("dep %s of %s exists", dep, e.code()).isTrue();
            }
        }
    }

    @Test
    void require_unknownCode_throws() {
        assertThatThrownBy(() -> ModuleCatalog.require("not-a-module"))
                .isInstanceOf(ModuleActivationStateException.class);
    }

    @Test
    void invalidCodeFormat_rejected() {
        assertThatThrownBy(() -> ModuleCatalogEntry.of("BAD CODE!", "n", "c",
                BillingTier.FREE, java.util.List.of(), false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
