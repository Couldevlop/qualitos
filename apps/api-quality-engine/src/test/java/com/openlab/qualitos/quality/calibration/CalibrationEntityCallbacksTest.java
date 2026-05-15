package com.openlab.qualitos.quality.calibration;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalibrationEntityCallbacksTest {

    @Test
    void equipmentPrePersist_defaultsActiveStatus() throws Exception {
        CalibrationEquipment e = new CalibrationEquipment();
        invoke(e, "prePersist");
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.ACTIVE);
        assertThat(e.getCreatedAt()).isNotNull();
    }

    @Test
    void equipmentPrePersist_preservesStatus() throws Exception {
        CalibrationEquipment e = new CalibrationEquipment();
        e.setStatus(EquipmentStatus.OUT_OF_SERVICE);
        invoke(e, "prePersist");
        assertThat(e.getStatus()).isEqualTo(EquipmentStatus.OUT_OF_SERVICE);
    }

    @Test
    void equipmentPreUpdate_refreshes() throws Exception {
        CalibrationEquipment e = new CalibrationEquipment();
        e.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = e.getUpdatedAt();
        Thread.sleep(5);
        invoke(e, "preUpdate");
        assertThat(e.getUpdatedAt()).isAfter(before);
    }

    @Test
    void planIsOverdue() {
        CalibrationPlan p = new CalibrationPlan();
        p.setNextDueOn(LocalDate.parse("2026-01-01"));
        assertThat(p.isOverdue(LocalDate.parse("2026-06-01"))).isTrue();
        assertThat(p.isOverdue(LocalDate.parse("2025-12-01"))).isFalse();
        p.setNextDueOn(null);
        assertThat(p.isOverdue(LocalDate.parse("2999-01-01"))).isFalse();
    }

    @Test
    void planPrePersist_stamps() throws Exception {
        CalibrationPlan p = new CalibrationPlan();
        invoke(p, "prePersist");
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    void recordPrePersist_stamps() throws Exception {
        CalibrationRecord r = new CalibrationRecord();
        invoke(r, "prePersist");
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    void msaPrePersist_stamps() throws Exception {
        MsaStudy m = new MsaStudy();
        invoke(m, "prePersist");
        assertThat(m.getCreatedAt()).isNotNull();
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
