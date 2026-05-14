package com.openlab.qualitos.quality.standards;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StandardsEntityCallbacksTest {

    @Test
    void standardPrePersist_defaults() throws Exception {
        Standard s = new Standard();
        invoke(s, "prePersist");
        assertThat(s.getStatus()).isEqualTo(StandardStatus.PUBLISHED);
        assertThat(s.getCreatedAt()).isNotNull();
    }

    @Test
    void standardPrePersist_preservesStatus() throws Exception {
        Standard s = new Standard();
        s.setStatus(StandardStatus.DEPRECATED);
        invoke(s, "prePersist");
        assertThat(s.getStatus()).isEqualTo(StandardStatus.DEPRECATED);
    }

    @Test
    void standardPreUpdate_refreshes() throws Exception {
        Standard s = new Standard();
        s.setCreatedAt(Instant.now().minusSeconds(60));
        s.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = s.getUpdatedAt();
        Thread.sleep(5);
        invoke(s, "preUpdate");
        assertThat(s.getUpdatedAt()).isAfter(before);
    }

    @Test
    void tenantStandardPrePersist_defaults() throws Exception {
        TenantStandard ts = new TenantStandard();
        invoke(ts, "prePersist");
        assertThat(ts.getStatus()).isEqualTo(AdoptionStatus.PLANNING);
        assertThat(ts.getCreatedAt()).isNotNull();
    }

    @Test
    void tenantStandardPrePersist_preservesStatus() throws Exception {
        TenantStandard ts = new TenantStandard();
        ts.setStatus(AdoptionStatus.CERTIFIED);
        invoke(ts, "prePersist");
        assertThat(ts.getStatus()).isEqualTo(AdoptionStatus.CERTIFIED);
    }

    @Test
    void tenantStandardPreUpdate_refreshes() throws Exception {
        TenantStandard ts = new TenantStandard();
        ts.setCreatedAt(Instant.now().minusSeconds(60));
        ts.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = ts.getUpdatedAt();
        Thread.sleep(5);
        invoke(ts, "preUpdate");
        assertThat(ts.getUpdatedAt()).isAfter(before);
    }

    @Test
    void evidencePrePersist_setsLinkedAt() throws Exception {
        RequirementEvidence ev = new RequirementEvidence();
        invoke(ev, "prePersist");
        assertThat(ev.getLinkedAt()).isNotNull();
    }

    @Test
    void evidencePrePersist_preservesExplicit() throws Exception {
        RequirementEvidence ev = new RequirementEvidence();
        Instant fixed = Instant.now().minusSeconds(300);
        ev.setLinkedAt(fixed);
        invoke(ev, "prePersist");
        assertThat(ev.getLinkedAt()).isEqualTo(fixed);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
