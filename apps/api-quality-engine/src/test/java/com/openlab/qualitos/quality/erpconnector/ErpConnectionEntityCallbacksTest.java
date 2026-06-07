package com.openlab.qualitos.quality.erpconnector;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ErpConnectionEntityCallbacksTest {

    @Test
    void prePersist_setsDefaults() throws Exception {
        ErpConnection c = new ErpConnection();
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ErpConnectionStatus.ACTIVE);
        assertThat(c.getCreatedAt()).isNotNull();
        assertThat(c.getUpdatedAt()).isNotNull();
    }

    @Test
    void prePersist_preservesExistingStatus() throws Exception {
        ErpConnection c = new ErpConnection();
        c.setStatus(ErpConnectionStatus.DISABLED);
        c.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        c.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        invoke(c, "prePersist");
        assertThat(c.getStatus()).isEqualTo(ErpConnectionStatus.DISABLED);
        assertThat(c.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void preUpdate_refreshesUpdatedAt() throws Exception {
        ErpConnection c = new ErpConnection();
        c.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = c.getUpdatedAt();
        Thread.sleep(5);
        invoke(c, "preUpdate");
        assertThat(c.getUpdatedAt()).isAfter(before);
    }

    @Test
    void notFoundException_hasErpSpecificMessage_and404Mapping() {
        UUID id = UUID.randomUUID();
        ErpConnectionNotFoundException ex = new ErpConnectionNotFoundException(id);
        assertThat(ex.getMessage()).contains("ERP connection not found").contains(id.toString());
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
