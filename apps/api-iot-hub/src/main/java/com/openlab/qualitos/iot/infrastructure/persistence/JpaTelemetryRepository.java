package com.openlab.qualitos.iot.infrastructure.persistence;

import com.openlab.qualitos.iot.domain.model.TelemetryPoint;
import com.openlab.qualitos.iot.domain.port.TelemetryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class JpaTelemetryRepository implements TelemetryRepository {

  private final TelemetryJpaRepository jpa;

  public JpaTelemetryRepository(TelemetryJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  @Transactional
  public TelemetryPoint save(TelemetryPoint p) {
    return toDomain(jpa.save(toEntity(p)));
  }

  @Override
  @Transactional
  public List<TelemetryPoint> saveAll(List<TelemetryPoint> points) {
    List<TelemetryEntity> entities = new ArrayList<>(points.size());
    for (TelemetryPoint p : points) entities.add(toEntity(p));
    List<TelemetryEntity> saved = jpa.saveAll(entities);
    List<TelemetryPoint> out = new ArrayList<>(saved.size());
    for (TelemetryEntity e : saved) out.add(toDomain(e));
    return out;
  }

  @Override
  public List<TelemetryPoint> findByDevice(UUID tenantId, UUID deviceId, Instant from, Instant to, int limit) {
    return jpa.findRange(tenantId, deviceId, from, to,
            PageRequest.of(0, Math.min(Math.max(limit, 1), 1000)))
        .stream().map(this::toDomain).toList();
  }

  @Override
  public long countByTenant(UUID tenantId) {
    return jpa.countByTenantId(tenantId);
  }

  private TelemetryEntity toEntity(TelemetryPoint p) {
    TelemetryEntity e = new TelemetryEntity();
    e.setId(p.id() == null ? UUID.randomUUID() : p.id());
    e.setTenantId(p.tenantId());
    e.setDeviceId(p.deviceId());
    e.setMetric(p.metric());
    e.setValue(p.value());
    e.setUnit(p.unit());
    e.setRecordedAt(p.recordedAt());
    return e;
  }

  private TelemetryPoint toDomain(TelemetryEntity e) {
    return new TelemetryPoint(
        e.getId(), e.getTenantId(), e.getDeviceId(),
        e.getMetric(), e.getValue(), e.getUnit(), e.getRecordedAt());
  }
}
