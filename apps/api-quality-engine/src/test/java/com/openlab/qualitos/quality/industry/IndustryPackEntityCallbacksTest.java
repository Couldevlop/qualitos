package com.openlab.qualitos.quality.industry;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IndustryPackEntityCallbacksTest {

    @Test
    void prePersist_setsTimestamps() throws Exception {
        IndustryPack p = new IndustryPack();
        invoke(p, "prePersist");
        assertThat(p.getCreatedAt()).isNotNull();
        assertThat(p.getUpdatedAt()).isNotNull();
    }

    @Test
    void prePersist_preservesExistingTimestamps() throws Exception {
        IndustryPack p = new IndustryPack();
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");
        p.setCreatedAt(ts);
        p.setUpdatedAt(ts);
        invoke(p, "prePersist");
        assertThat(p.getCreatedAt()).isEqualTo(ts);
    }

    @Test
    void preUpdate_refreshes() throws Exception {
        IndustryPack p = new IndustryPack();
        p.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = p.getUpdatedAt();
        Thread.sleep(5);
        invoke(p, "preUpdate");
        assertThat(p.getUpdatedAt()).isAfter(before);
    }

    @Test
    void activationPrePersist_defaultsStatusAndStamp() throws Exception {
        TenantIndustryPackActivation a = new TenantIndustryPackActivation();
        invoke(a, "prePersist");
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.ACTIVE);
        assertThat(a.getActivatedAt()).isNotNull();
    }

    @Test
    void activationPrePersist_preservesExistingStatus() throws Exception {
        TenantIndustryPackActivation a = new TenantIndustryPackActivation();
        a.setStatus(ActivationStatus.DEACTIVATED);
        invoke(a, "prePersist");
        assertThat(a.getStatus()).isEqualTo(ActivationStatus.DEACTIVATED);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
