package com.openlab.qualitos.quality.dashboards.annotations.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardAnnotationTest {

    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-20T10:00:00Z");

    @Test
    void create_trimsBodyAndAnchor_andHasNoId() {
        DashboardAnnotation a = DashboardAnnotation.create(
                TENANT, AUTHOR, "exec.trend", "  Mai  ", "  Dérive nette  ", NOW);
        assertThat(a.getId()).isNull();
        assertThat(a.getTenantId()).isEqualTo(TENANT);
        assertThat(a.getAuthorId()).isEqualTo(AUTHOR);
        assertThat(a.getChartKey()).isEqualTo("exec.trend");
        assertThat(a.getAnchorLabel()).isEqualTo("Mai");
        assertThat(a.getBody()).isEqualTo("Dérive nette");
        assertThat(a.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void blankAnchor_becomesNull() {
        DashboardAnnotation a = DashboardAnnotation.create(
                TENANT, AUTHOR, "exec.trend", "   ", "x", NOW);
        assertThat(a.getAnchorLabel()).isNull();
    }

    @Test
    void nullAnchor_isAllowed() {
        DashboardAnnotation a = DashboardAnnotation.create(
                TENANT, AUTHOR, "exec.pareto", null, "x", NOW);
        assertThat(a.getAnchorLabel()).isNull();
    }

    @Test
    void assignId_setsId_andRejectsNull() {
        DashboardAnnotation a = DashboardAnnotation.create(TENANT, AUTHOR, "k.k", null, "b", NOW);
        UUID id = UUID.randomUUID();
        a.assignId(id);
        assertThat(a.getId()).isEqualTo(id);
        assertThatThrownBy(() -> a.assignId(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isAuthoredBy_matchesAuthorOnly() {
        DashboardAnnotation a = DashboardAnnotation.create(TENANT, AUTHOR, "k.k", null, "b", NOW);
        assertThat(a.isAuthoredBy(AUTHOR)).isTrue();
        assertThat(a.isAuthoredBy(UUID.randomUUID())).isFalse();
    }

    @Test
    void rejects_nullTenant() {
        assertThatThrownBy(() -> DashboardAnnotation.create(null, AUTHOR, "k.k", null, "b", NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_nullAuthor() {
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, null, "k.k", null, "b", NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_nullCreatedAt() {
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, "k.k", null, "b", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejects_badChartKey() {
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, "Exec Trend", null, "b", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, "", null, "b", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, null, null, "b", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_blankOrNullBody() {
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, "k.k", null, "   ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, "k.k", null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_tooLongBody() {
        String tooLong = "x".repeat(DashboardAnnotation.BODY_MAX + 1);
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, "k.k", null, tooLong, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_tooLongAnchor() {
        String tooLong = "x".repeat(DashboardAnnotation.LABEL_MAX + 1);
        assertThatThrownBy(() -> DashboardAnnotation.create(TENANT, AUTHOR, "k.k", tooLong, "b", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_maxLengthBodyAndAnchor() {
        String body = "x".repeat(DashboardAnnotation.BODY_MAX);
        String anchor = "y".repeat(DashboardAnnotation.LABEL_MAX);
        DashboardAnnotation a = DashboardAnnotation.create(TENANT, AUTHOR, "a1.b_2-c", anchor, body, NOW);
        assertThat(a.getBody()).hasSize(DashboardAnnotation.BODY_MAX);
        assertThat(a.getAnchorLabel()).hasSize(DashboardAnnotation.LABEL_MAX);
    }
}
