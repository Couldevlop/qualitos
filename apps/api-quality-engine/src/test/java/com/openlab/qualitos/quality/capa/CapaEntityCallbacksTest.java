package com.openlab.qualitos.quality.capa;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CapaEntityCallbacksTest {

    @Test
    void casePrePersist_defaults() throws Exception {
        CapaCase c = new CapaCase();
        invoke(c, "prePersist");
        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getUpdatedAt()).isNotNull();
        assertThat(c.getStatus()).isEqualTo(CapaStatus.OPEN);
    }

    @Test
    void casePrePersist_preservesExplicitStatus() throws Exception {
        CapaCase c = new CapaCase();
        c.setStatus(CapaStatus.IN_PROGRESS);
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(CapaStatus.IN_PROGRESS);
    }

    @Test
    void casePreUpdate_refreshes() throws Exception {
        CapaCase c = new CapaCase();
        c.setCreatedAt(Instant.now().minusSeconds(60));
        c.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = c.getUpdatedAt();
        Thread.sleep(5);
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isAfter(before);
    }

    @Test
    void actionPrePersist_defaults() throws Exception {
        CapaAction a = new CapaAction();
        invoke(a, "prePersist");
        assertThat(a.getStatus()).isEqualTo(CapaActionStatus.PENDING);
        assertThat(a.getCreatedAt()).isNotNull();
    }

    @Test
    void actionPrePersist_preservesStatus() throws Exception {
        CapaAction a = new CapaAction();
        a.setStatus(CapaActionStatus.DONE);
        invoke(a, "prePersist");
        assertThat(a.getStatus()).isEqualTo(CapaActionStatus.DONE);
    }

    @Test
    void actionPreUpdate_refreshes() throws Exception {
        CapaAction a = new CapaAction();
        a.setCreatedAt(Instant.now().minusSeconds(60));
        a.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = a.getUpdatedAt();
        Thread.sleep(5);
        invoke(a, "preUpdate");
        assertThat(a.getUpdatedAt()).isAfter(before);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
