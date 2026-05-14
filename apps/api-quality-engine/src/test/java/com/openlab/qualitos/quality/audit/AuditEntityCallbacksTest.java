package com.openlab.qualitos.quality.audit;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEntityCallbacksTest {

    @Test
    void planPrePersist_defaults() throws Exception {
        AuditPlan p = new AuditPlan();
        invoke(p, "prePersist");
        assertThat(p.getStatus()).isEqualTo(AuditStatus.PLANNED);
        assertThat(p.getCreatedAt()).isNotNull();
    }

    @Test
    void planPrePersist_preservesStatus() throws Exception {
        AuditPlan p = new AuditPlan();
        p.setStatus(AuditStatus.IN_PROGRESS);
        invoke(p, "prePersist");
        assertThat(p.getStatus()).isEqualTo(AuditStatus.IN_PROGRESS);
    }

    @Test
    void planPreUpdate_refreshes() throws Exception {
        AuditPlan p = new AuditPlan();
        p.setCreatedAt(Instant.now().minusSeconds(60));
        p.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = p.getUpdatedAt();
        Thread.sleep(5);
        invoke(p, "preUpdate");
        assertThat(p.getUpdatedAt()).isAfter(before);
    }

    @Test
    void itemPrePersist_defaultsWeight() throws Exception {
        AuditChecklistItem i = new AuditChecklistItem();
        invoke(i, "prePersist");
        assertThat(i.getWeight()).isEqualTo(1);
        assertThat(i.getCreatedAt()).isNotNull();
    }

    @Test
    void itemPrePersist_preservesWeight() throws Exception {
        AuditChecklistItem i = new AuditChecklistItem();
        i.setWeight(7);
        invoke(i, "prePersist");
        assertThat(i.getWeight()).isEqualTo(7);
    }

    @Test
    void itemPreUpdate_refreshes() throws Exception {
        AuditChecklistItem i = new AuditChecklistItem();
        i.setCreatedAt(Instant.now().minusSeconds(60));
        i.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = i.getUpdatedAt();
        Thread.sleep(5);
        invoke(i, "preUpdate");
        assertThat(i.getUpdatedAt()).isAfter(before);
    }

    @Test
    void findingPrePersist_setsRaisedAt() throws Exception {
        AuditFinding f = new AuditFinding();
        invoke(f, "prePersist");
        assertThat(f.getRaisedAt()).isNotNull();
        assertThat(f.getCreatedAt()).isNotNull();
    }

    @Test
    void findingPrePersist_preservesRaisedAt() throws Exception {
        AuditFinding f = new AuditFinding();
        Instant fixed = Instant.now().minusSeconds(300);
        f.setRaisedAt(fixed);
        invoke(f, "prePersist");
        assertThat(f.getRaisedAt()).isEqualTo(fixed);
    }

    @Test
    void findingPreUpdate_refreshes() throws Exception {
        AuditFinding f = new AuditFinding();
        f.setCreatedAt(Instant.now().minusSeconds(60));
        f.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = f.getUpdatedAt();
        Thread.sleep(5);
        invoke(f, "preUpdate");
        assertThat(f.getUpdatedAt()).isAfter(before);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
