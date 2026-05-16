package com.openlab.qualitos.quality.dmaic;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DmaicEntityCallbacksTest {

    @Test
    void projectPrePersist_defaults() throws Exception {
        DmaicProject p = new DmaicProject();
        invoke(p, "prePersist");
        assertThat(p.getPhase()).isEqualTo(DmaicPhase.DEFINE);
        assertThat(p.getStatus()).isEqualTo(DmaicStatus.ACTIVE);
        assertThat(p.getStartedAt()).isNotNull();
        assertThat(p.getCreatedAt()).isNotNull();
    }

    @Test
    void projectPrePersist_preserves() throws Exception {
        DmaicProject p = new DmaicProject();
        p.setPhase(DmaicPhase.IMPROVE);
        p.setStatus(DmaicStatus.ON_HOLD);
        Instant fixed = Instant.now().minusSeconds(60);
        p.setStartedAt(fixed);
        invoke(p, "prePersist");
        assertThat(p.getPhase()).isEqualTo(DmaicPhase.IMPROVE);
        assertThat(p.getStatus()).isEqualTo(DmaicStatus.ON_HOLD);
        assertThat(p.getStartedAt()).isEqualTo(fixed);
    }

    @Test
    void projectPreUpdate_refreshes() throws Exception {
        DmaicProject p = new DmaicProject();
        p.setCreatedAt(Instant.now().minusSeconds(60));
        p.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = p.getUpdatedAt();
        Thread.sleep(5);
        invoke(p, "preUpdate");
        assertThat(p.getUpdatedAt()).isAfter(before);
    }

    @Test
    void measurePrePersist_setsRecordedIfNull() throws Exception {
        ProcessMeasure m = new ProcessMeasure();
        invoke(m, "prePersist");
        assertThat(m.getCreatedAt()).isNotNull();
        assertThat(m.getRecordedAt()).isNotNull();
    }

    @Test
    void measurePrePersist_preservesRecordedAt() throws Exception {
        ProcessMeasure m = new ProcessMeasure();
        Instant fixed = Instant.now().minusSeconds(300);
        m.setRecordedAt(fixed);
        invoke(m, "prePersist");
        assertThat(m.getRecordedAt()).isEqualTo(fixed);
    }

    @Test
    void devicePrePersist_setsTimestamps() throws Exception {
        PokaYokeDevice d = new PokaYokeDevice();
        invoke(d, "prePersist");
        assertThat(d.getCreatedAt()).isNotNull();
        assertThat(d.getUpdatedAt()).isNotNull();
    }

    @Test
    void devicePreUpdate_refreshes() throws Exception {
        PokaYokeDevice d = new PokaYokeDevice();
        d.setCreatedAt(Instant.now().minusSeconds(60));
        d.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = d.getUpdatedAt();
        Thread.sleep(5);
        invoke(d, "preUpdate");
        assertThat(d.getUpdatedAt()).isAfter(before);
    }

    @Test
    void assignmentPrePersist_defaultsProposed() throws Exception {
        PokaYokeAssignment a = new PokaYokeAssignment();
        invoke(a, "prePersist");
        assertThat(a.getStatus()).isEqualTo(PokaYokeAssignmentStatus.PROPOSED);
    }

    @Test
    void assignmentPrePersist_preservesStatus() throws Exception {
        PokaYokeAssignment a = new PokaYokeAssignment();
        a.setStatus(PokaYokeAssignmentStatus.IN_DESIGN);
        invoke(a, "prePersist");
        assertThat(a.getStatus()).isEqualTo(PokaYokeAssignmentStatus.IN_DESIGN);
    }

    @Test
    void assignmentPreUpdate_refreshes() throws Exception {
        PokaYokeAssignment a = new PokaYokeAssignment();
        a.setCreatedAt(Instant.now().minusSeconds(60));
        a.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = a.getUpdatedAt();
        Thread.sleep(5);
        invoke(a, "preUpdate");
        assertThat(a.getUpdatedAt()).isAfter(before);
    }

    @Test
    void dmaicPhase_next_chain() {
        assertThat(DmaicPhase.DEFINE.next()).isEqualTo(DmaicPhase.MEASURE);
        assertThat(DmaicPhase.MEASURE.next()).isEqualTo(DmaicPhase.ANALYZE);
        assertThat(DmaicPhase.ANALYZE.next()).isEqualTo(DmaicPhase.IMPROVE);
        assertThat(DmaicPhase.IMPROVE.next()).isEqualTo(DmaicPhase.CONTROL);
        assertThat(DmaicPhase.CONTROL.next()).isNull();
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
