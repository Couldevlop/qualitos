package com.openlab.qualitos.iot.infrastructure.external;

import com.openlab.qualitos.iot.domain.model.Threshold;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryThresholdRegistryTest {

    private final InMemoryThresholdRegistry registry = new InMemoryThresholdRegistry();
    private final UUID tenant = UUID.randomUUID();
    private final UUID device = UUID.randomUUID();

    @Test
    void findFor_unknownKey_returnsEmptyList() {
        assertThat(registry.findFor(tenant, device, "temperature")).isEmpty();
    }

    @Test
    void register_thenFindFor_returnsThreshold() {
        Threshold t = new Threshold("temperature", 0.0, 100.0, Threshold.Severity.WARNING);
        registry.register(tenant, device, t);
        List<Threshold> found = registry.findFor(tenant, device, "temperature");
        assertThat(found).containsExactly(t);
    }

    @Test
    void register_multipleThresholdsForSameKey_areAccumulated() {
        Threshold warn = new Threshold("vibration", null, 50.0, Threshold.Severity.WARNING);
        Threshold crit = new Threshold("vibration", null, 80.0, Threshold.Severity.CRITICAL);
        registry.register(tenant, device, warn);
        registry.register(tenant, device, crit);
        assertThat(registry.findFor(tenant, device, "vibration"))
                .containsExactly(warn, crit);
    }

    @Test
    void register_scopedByDevice() {
        UUID otherDevice = UUID.randomUUID();
        Threshold t = new Threshold("rpm", 100.0, null, Threshold.Severity.INFO);
        registry.register(tenant, device, t);
        assertThat(registry.findFor(tenant, otherDevice, "rpm")).isEmpty();
        assertThat(registry.findFor(tenant, device, "rpm")).containsExactly(t);
    }
}
