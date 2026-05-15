package com.openlab.qualitos.quality.itsm;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ItsmConnectionEntityCallbacksTest {

    @Test
    void prePersist_setsDefaults() throws Exception {
        ItsmConnection c = new ItsmConnection();
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.ACTIVE);
        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void prePersist_preservesExistingStatus() throws Exception {
        ItsmConnection c = new ItsmConnection();
        c.setStatus(ConnectionStatus.DISABLED);
        c.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        c.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ConnectionStatus.DISABLED);
        assertThat(c.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void preUpdate_refreshesUpdatedAt() throws Exception {
        ItsmConnection c = new ItsmConnection();
        c.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = c.getUpdatedAt();
        Thread.sleep(5);
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isAfter(before);
    }

    @Test
    void mappingPrePersist_setsTimestamps() throws Exception {
        ItsmIncidentMapping m = new ItsmIncidentMapping();
        invoke(m, "prePersist");
        assertThat(m.getFirstImportedAt()).isNotNull();
        assertThat(m.getLastSeenAt()).isNotNull();
    }

    @Test
    void mappingPrePersist_preservesExistingTimestamps() throws Exception {
        ItsmIncidentMapping m = new ItsmIncidentMapping();
        Instant ts = Instant.parse("2026-02-02T02:02:02Z");
        m.setFirstImportedAt(ts);
        m.setLastSeenAt(ts);
        invoke(m, "prePersist");
        assertThat(m.getFirstImportedAt()).isEqualTo(ts);
        assertThat(m.getLastSeenAt()).isEqualTo(ts);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
