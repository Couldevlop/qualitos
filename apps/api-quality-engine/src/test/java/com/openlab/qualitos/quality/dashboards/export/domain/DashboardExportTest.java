package com.openlab.qualitos.quality.dashboards.export.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardExportTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID DASH = UUID.randomUUID();
    static final String SHA = "a".repeat(64);
    static final String CODE = "abcDEF012345_-xy";
    static final Instant NOW = Instant.parse("2026-06-22T10:00:00Z");

    @Test
    void create_setsAllFields_andNoId() {
        DashboardExport e = DashboardExport.create(
                TENANT, USER, DASH, "Exec", CODE, SHA, "env", "tx-1", NOW);
        assertThat(e.getId()).isNull();
        assertThat(e.getTenantId()).isEqualTo(TENANT);
        assertThat(e.getUserId()).isEqualTo(USER);
        assertThat(e.getDashboardId()).isEqualTo(DASH);
        assertThat(e.getDashboardName()).isEqualTo("Exec");
        assertThat(e.getVerificationCode()).isEqualTo(CODE);
        assertThat(e.getSha256Hex()).isEqualTo(SHA);
        assertThat(e.getSignatureEnvelope()).isEqualTo("env");
        assertThat(e.getAnchorTxRef()).isEqualTo("tx-1");
        assertThat(e.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void assignId_sets() {
        DashboardExport e = DashboardExport.create(TENANT, USER, DASH, "n", CODE, SHA, "env", "tx", NOW);
        UUID id = UUID.randomUUID();
        e.assignId(id);
        assertThat(e.getId()).isEqualTo(id);
    }

    @Test
    void rejects_badSha() {
        assertThatThrownBy(() -> DashboardExport.create(
                TENANT, USER, DASH, "n", CODE, "XYZ", "env", "tx", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sha256");
    }

    @Test
    void rejects_badCode() {
        assertThatThrownBy(() -> DashboardExport.create(
                TENANT, USER, DASH, "n", "short", SHA, "env", "tx", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("verificationCode");
    }

    @Test
    void rejects_blankName() {
        assertThatThrownBy(() -> DashboardExport.create(
                TENANT, USER, DASH, "  ", CODE, SHA, "env", "tx", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dashboardName");
    }

    @Test
    void rejects_blankSignature() {
        assertThatThrownBy(() -> DashboardExport.create(
                TENANT, USER, DASH, "n", CODE, SHA, "", "tx", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signatureEnvelope");
    }

    @Test
    void rejects_blankAnchor() {
        assertThatThrownBy(() -> DashboardExport.create(
                TENANT, USER, DASH, "n", CODE, SHA, "env", "  ", NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anchorTxRef");
    }

    @Test
    void rejects_nullTenant() {
        assertThatThrownBy(() -> DashboardExport.create(
                null, USER, DASH, "n", CODE, SHA, "env", "tx", NOW))
                .isInstanceOf(NullPointerException.class);
    }
}
