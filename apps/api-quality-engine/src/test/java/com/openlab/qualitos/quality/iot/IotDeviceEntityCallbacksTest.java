package com.openlab.qualitos.quality.iot;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class IotDeviceEntityCallbacksTest {

    @Test
    void devicePrePersist_defaultsStatusAndStamps() throws Exception {
        IotDevice d = new IotDevice();
        invoke(d, "prePersist");
        assertThat(d.getStatus()).isEqualTo(IotDeviceStatus.PROVISIONED);
        assertThat(d.getCreatedAt()).isNotNull();
        assertThat(d.getUpdatedAt()).isNotNull();
    }

    @Test
    void devicePrePersist_preservesStatus() throws Exception {
        IotDevice d = new IotDevice();
        d.setStatus(IotDeviceStatus.ACTIVE);
        invoke(d, "prePersist");
        assertThat(d.getStatus()).isEqualTo(IotDeviceStatus.ACTIVE);
    }

    @Test
    void devicePreUpdate_refreshes() throws Exception {
        IotDevice d = new IotDevice();
        d.setUpdatedAt(Instant.now().minusSeconds(60));
        Instant before = d.getUpdatedAt();
        Thread.sleep(5);
        invoke(d, "preUpdate");
        assertThat(d.getUpdatedAt()).isAfter(before);
    }

    @Test
    void telemetryPrePersist_defaultsSourceAndStamps() throws Exception {
        IotTelemetryEvent e = new IotTelemetryEvent();
        invoke(e, "prePersist");
        assertThat(e.getSource()).isEqualTo(IotProtocol.MANUAL);
        assertThat(e.getRecordedAt()).isNotNull();
        assertThat(e.getIngestedAt()).isNotNull();
    }

    @Test
    void telemetryPrePersist_preservesProvidedTimestamps() throws Exception {
        IotTelemetryEvent e = new IotTelemetryEvent();
        Instant ts = Instant.parse("2026-04-04T04:04:04Z");
        e.setRecordedAt(ts);
        invoke(e, "prePersist");
        assertThat(e.getRecordedAt()).isEqualTo(ts);
    }

    private static void invoke(Object t, String m) throws Exception {
        Method method = t.getClass().getDeclaredMethod(m);
        method.setAccessible(true);
        method.invoke(t);
    }
}
