package com.openlab.qualitos.iot.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "iot_telemetry")
public class TelemetryEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private UUID tenantId;

  @Column(name = "device_id", nullable = false, updatable = false)
  private UUID deviceId;

  @Column(name = "metric", nullable = false, length = 100)
  private String metric;

  @Column(name = "value_double") private Double value;
  @Column(name = "unit", length = 32) private String unit;

  @Column(name = "recorded_at", nullable = false) private Instant recordedAt;

  protected TelemetryEntity() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getTenantId() { return tenantId; }
  public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
  public UUID getDeviceId() { return deviceId; }
  public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
  public String getMetric() { return metric; }
  public void setMetric(String metric) { this.metric = metric; }
  public Double getValue() { return value; }
  public void setValue(Double value) { this.value = value; }
  public String getUnit() { return unit; }
  public void setUnit(String unit) { this.unit = unit; }
  public Instant getRecordedAt() { return recordedAt; }
  public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
}
