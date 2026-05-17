package com.openlab.qualitos.quality.dashboards.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardLayoutTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-05-16T10:00:00Z");

    @Test
    void create_defaultsVersionTo1() {
        DashboardLayout l = DashboardLayout.create(TENANT, USER, "Exec view",
                "desc", "{\"grid\":[]}", true, NOW);
        assertThat(l.getVersion()).isEqualTo(1);
        assertThat(l.isShared()).isTrue();
        assertThat(l.getTenantId()).isEqualTo(TENANT);
        assertThat(l.getSignatureHash()).isNull();
    }

    @Test
    void update_incrementsVersionAndResetsSignature() {
        DashboardLayout l = DashboardLayout.create(TENANT, USER, "v1",
                null, "{\"grid\":[]}", false, NOW);
        l.attachSignature("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        l.update("v2", null, "{\"grid\":[1]}", true, NOW.plusSeconds(1));
        assertThat(l.getVersion()).isEqualTo(2);
        assertThat(l.getName()).isEqualTo("v2");
        assertThat(l.getSignatureHash()).isNull();
    }

    @Test
    void invalidName_throws() {
        assertThatThrownBy(() -> DashboardLayout.create(TENANT, USER, "x",
                null, "{}", false, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonObjectJson_throws() {
        assertThatThrownBy(() -> DashboardLayout.create(TENANT, USER, "good name",
                null, "[1,2]", false, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankJson_throws() {
        assertThatThrownBy(() -> DashboardLayout.create(TENANT, USER, "good name",
                null, "  ", false, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shortSignature_rejected() {
        DashboardLayout l = DashboardLayout.create(TENANT, USER, "name", null, "{}", false, NOW);
        assertThatThrownBy(() -> l.attachSignature("short"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
