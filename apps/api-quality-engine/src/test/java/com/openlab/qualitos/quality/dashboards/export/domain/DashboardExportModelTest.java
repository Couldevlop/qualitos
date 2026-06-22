package com.openlab.qualitos.quality.dashboards.export.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardExportModelTest {

    @Test
    void widget_nullTypeAndLines_normalisedToEmpty() {
        var w = new DashboardExportModel.Widget("t", null, null);
        assertThat(w.type()).isEmpty();
        assertThat(w.dataLines()).isEmpty();
    }

    @Test
    void model_nullWidgets_normalisedToEmpty() {
        var m = new DashboardExportModel("N", "t", "d",
                Instant.parse("2026-06-22T10:00:00Z"), null, "abcDEF012345_-xy");
        assertThat(m.widgets()).isEmpty();
    }

    @Test
    void model_immutableWidgets() {
        var widgets = new java.util.ArrayList<DashboardExportModel.Widget>();
        widgets.add(new DashboardExportModel.Widget("a", "kpi", List.of("x")));
        var m = new DashboardExportModel("N", "t", "d",
                Instant.parse("2026-06-22T10:00:00Z"), widgets, "abcDEF012345_-xy");
        assertThatThrownBy(() -> m.widgets().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void model_requiresName() {
        assertThatThrownBy(() -> new DashboardExportModel(null, "t", "d",
                Instant.now(), List.of(), "code0123456789ab"))
                .isInstanceOf(NullPointerException.class);
    }
}
