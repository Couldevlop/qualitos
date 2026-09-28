package com.openlab.qualitos.quality.costofquality.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoqLabelTest {

    static final UUID TENANT = UUID.randomUUID();

    @Test
    void unLibelleSaisiAppartientAuTenantSansCodeEtSeRangeApresLeCatalogue() {
        CoqLabel l = CoqLabel.custom(TENANT, CoqCategory.APPRAISAL, "  Tri 100 % client  ", true);

        assertThat(l.getTenantId()).isEqualTo(TENANT);
        assertThat(l.getCode()).isNull();
        assertThat(l.getName()).isEqualTo("Tri 100 % client");
        assertThat(l.isPartControl()).isTrue();
        assertThat(l.getPosition()).isEqualTo(CoqLabel.CUSTOM_POSITION);
        assertThat(l.builtIn()).isFalse();
    }

    @Test
    void unLibelleLivreNAPasDeTenant() {
        CoqLabel l = new CoqLabel(UUID.randomUUID(), null, CoqCategory.PREVENTION, "X", "X", false, 10);
        assertThat(l.builtIn()).isTrue();
    }

    @Test
    void unLibelleVideTropLongOuSansFamilleEstRefuse() {
        assertThatThrownBy(() -> CoqLabel.custom(TENANT, CoqCategory.APPRAISAL, "  ", false))
                .isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> CoqLabel.custom(TENANT, CoqCategory.APPRAISAL, null, false))
                .isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> CoqLabel.custom(TENANT, CoqCategory.APPRAISAL, "x".repeat(151), false))
                .isInstanceOf(CoqValidationException.class);
        assertThatThrownBy(() -> CoqLabel.custom(TENANT, null, "Tri", false))
                .isInstanceOf(CoqValidationException.class);
    }

    @Test
    void lesFamillesSeRepartissentEntreConformiteEtNonConformite() {
        assertThat(CoqCategory.PREVENTION.conformance()).isTrue();
        assertThat(CoqCategory.APPRAISAL.conformance()).isTrue();
        assertThat(CoqCategory.INTERNAL_FAILURE.conformance()).isFalse();
        assertThat(CoqCategory.EXTERNAL_FAILURE.conformance()).isFalse();
    }
}
