package com.openlab.qualitos.quality.risk;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FmeaItemRpnTest {

    @Test
    void recomputeRpn_multipliesSeverityOccurrenceDetection() {
        FmeaItem i = new FmeaItem();
        i.setSeverity(7); i.setOccurrence(3); i.setDetection(4);
        i.recomputeRpn();
        assertThat(i.getRpn()).isEqualTo(84);
    }

    @Test
    void recomputeRpn_rpnAfter_nullWhenAnyResultingMissing() {
        FmeaItem i = new FmeaItem();
        i.setSeverity(7); i.setOccurrence(3); i.setDetection(4);
        i.setResultingSeverity(2);
        i.setResultingOccurrence(2);
        // detection résultante manquante
        i.recomputeRpn();
        assertThat(i.getRpnAfter()).isNull();
    }

    @Test
    void recomputeRpn_rpnAfter_computedWhenAllPresent() {
        FmeaItem i = new FmeaItem();
        i.setSeverity(7); i.setOccurrence(3); i.setDetection(4);
        i.setResultingSeverity(2);
        i.setResultingOccurrence(2);
        i.setResultingDetection(2);
        i.recomputeRpn();
        assertThat(i.getRpnAfter()).isEqualTo(8);
        assertThat(i.getRpn()).isEqualTo(84);
    }

    @Test
    void prePersist_recomputesRpn_andStamps() throws Exception {
        FmeaItem i = new FmeaItem();
        i.setSeverity(5); i.setOccurrence(5); i.setDetection(5);
        invoke(i, "prePersist");
        assertThat(i.getRpn()).isEqualTo(125);
        assertThat(i.getCreatedAt()).isNotNull();
        assertThat(i.getUpdatedAt()).isNotNull();
    }

    @Test
    void preUpdate_recomputesRpn_andStamps() throws Exception {
        FmeaItem i = new FmeaItem();
        i.setSeverity(2); i.setOccurrence(2); i.setDetection(2);
        i.recomputeRpn();
        assertThat(i.getRpn()).isEqualTo(8);
        i.setSeverity(10); i.setOccurrence(10); i.setDetection(10);
        i.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = i.getUpdatedAt();
        Thread.sleep(5);
        invoke(i, "preUpdate");
        assertThat(i.getRpn()).isEqualTo(1000);
        assertThat(i.getUpdatedAt()).isAfter(before);
    }

    @Test
    void projectPrePersist_defaultsStatusThresholdRevision() throws Exception {
        FmeaProject p = new FmeaProject();
        invoke(p, "prePersist");
        assertThat(p.getStatus()).isEqualTo(FmeaStatus.DRAFT);
        assertThat(p.getCriticalRpnThreshold()).isEqualTo(100);
        assertThat(p.getRevision()).isEqualTo(1);
        assertThat(p.getCreatedAt()).isNotNull();
    }

    @Test
    void projectPrePersist_preservesExistingValues() throws Exception {
        FmeaProject p = new FmeaProject();
        p.setStatus(FmeaStatus.ACTIVE);
        p.setCriticalRpnThreshold(50);
        p.setRevision(3);
        invoke(p, "prePersist");
        assertThat(p.getStatus()).isEqualTo(FmeaStatus.ACTIVE);
        assertThat(p.getCriticalRpnThreshold()).isEqualTo(50);
        assertThat(p.getRevision()).isEqualTo(3);
    }

    @Test
    void projectPreUpdate_refreshes() throws Exception {
        FmeaProject p = new FmeaProject();
        p.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = p.getUpdatedAt();
        Thread.sleep(5);
        invoke(p, "preUpdate");
        assertThat(p.getUpdatedAt()).isAfter(before);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
