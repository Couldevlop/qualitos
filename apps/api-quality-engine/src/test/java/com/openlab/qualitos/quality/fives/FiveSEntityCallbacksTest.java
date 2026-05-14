package com.openlab.qualitos.quality.fives;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FiveSEntityCallbacksTest {

    @Test
    void auditPrePersist_setsDefaults() throws Exception {
        FiveSAudit a = new FiveSAudit();
        invoke(a, "prePersist");
        assertThat(a.getCreatedAt()).isNotNull();
        assertThat(a.getUpdatedAt()).isNotNull();
        assertThat(a.getStatus()).isEqualTo(FiveSAuditStatus.DRAFT);
    }

    @Test
    void auditPrePersist_preservesExplicitStatus() throws Exception {
        FiveSAudit a = new FiveSAudit();
        a.setStatus(FiveSAuditStatus.IN_PROGRESS);
        invoke(a, "prePersist");
        assertThat(a.getStatus()).isEqualTo(FiveSAuditStatus.IN_PROGRESS);
    }

    @Test
    void auditPreUpdate_refreshes() throws Exception {
        FiveSAudit a = new FiveSAudit();
        a.setCreatedAt(Instant.now().minusSeconds(60));
        a.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = a.getUpdatedAt();
        Thread.sleep(5);
        invoke(a, "preUpdate");
        assertThat(a.getUpdatedAt()).isAfter(before);
    }

    @Test
    void itemPrePersist_setsTimestamps() throws Exception {
        FiveSAuditItem i = new FiveSAuditItem();
        invoke(i, "prePersist");
        assertThat(i.getCreatedAt()).isNotNull();
        assertThat(i.getUpdatedAt()).isNotNull();
    }

    @Test
    void itemPreUpdate_refreshes() throws Exception {
        FiveSAuditItem i = new FiveSAuditItem();
        i.setCreatedAt(Instant.now().minusSeconds(60));
        i.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = i.getUpdatedAt();
        Thread.sleep(5);
        invoke(i, "preUpdate");
        assertThat(i.getUpdatedAt()).isAfter(before);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
