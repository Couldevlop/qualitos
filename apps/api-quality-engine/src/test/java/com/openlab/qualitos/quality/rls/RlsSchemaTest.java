package com.openlab.qualitos.quality.rls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RlsSchemaTest {

    @Test
    void conversionSelonLeTypeDeLaColonne() {
        assertThat(RlsSchema.castFor("uuid")).isEqualTo("::uuid");
        assertThat(RlsSchema.castFor("text")).isEmpty();
        assertThat(RlsSchema.castFor("character varying(64)")).isEmpty();
        assertThat(RlsSchema.castFor("bigint")).isNull();
    }

    @Test
    void nomDeRoleInjecteRefuseAvantToutEchange() {
        assertThatThrownBy(() -> RlsSchema.applyRole(null, "app; DROP TABLE x", "x".repeat(20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void motDePasseCourtRefuse() {
        assertThatThrownBy(() -> RlsSchema.applyRole(null, "qualitos_app", "court"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void proprietesNettoyees() {
        RlsProperties p = new RlsProperties();
        assertThat(p.hasAppUser()).isFalse();
        p.setAppUser("  qualitos_app ");
        p.setAppPassword(null);
        assertThat(p.getAppUser()).isEqualTo("qualitos_app");
        assertThat(p.getAppPassword()).isEmpty();
        assertThat(p.isEnabled()).isFalse();
    }
}
