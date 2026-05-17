package com.openlab.qualitos.iot.infrastructure.external;

import com.openlab.qualitos.iot.domain.model.Threshold;
import com.openlab.qualitos.iot.domain.port.ThresholdRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * V1 in-memory threshold registry. P4 will swap this for a
 * tenant-scoped DB-backed implementation driven by industry packs.
 */
@Component
public class InMemoryThresholdRegistry implements ThresholdRegistry {

  private final Map<String, List<Threshold>> store = new ConcurrentHashMap<>();

  @Override
  public List<Threshold> findFor(UUID tenantId, UUID deviceId, String metric) {
    return store.getOrDefault(key(tenantId, deviceId, metric), List.of());
  }

  public void register(UUID tenantId, UUID deviceId, Threshold t) {
    store.computeIfAbsent(key(tenantId, deviceId, t.metric()), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
        .add(t);
  }

  private static String key(UUID tenantId, UUID deviceId, String metric) {
    return tenantId + "|" + deviceId + "|" + metric;
  }
}
