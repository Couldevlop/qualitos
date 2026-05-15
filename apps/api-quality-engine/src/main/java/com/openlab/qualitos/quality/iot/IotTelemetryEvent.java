package com.openlab.qualitos.quality.iot;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Événement de télémétrie (CLAUDE.md §9.7).
 *
 * Le couple (valueNumeric, valueText) permet de stocker indifféremment des mesures
 * numériques (températures, vibrations…) et des valeurs catégorielles ("RUNNING",
 * "IDLE", code d'alarme). Le champ {@link #recordedAt} est la timestamp source
 * (capteur) ; {@link #ingestedAt} est l'horodatage côté Hub — la différence sert à
 * détecter les latences réseau et les retards de buffer Edge.
 *
 * Pour V1, table PostgreSQL normale. À terme : partition par mois ou bascule
 * TimescaleDB (cf. CLAUDE.md §6.4).
 */
@Entity
@Table(name = "iot_telemetry_events",
        indexes = {
                @Index(name = "idx_iot_tel_tenant_device",
                        columnList = "tenant_id, device_id, recorded_at"),
                @Index(name = "idx_iot_tel_tenant_metric",
                        columnList = "tenant_id, metric, recorded_at")
        })
public class IotTelemetryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(nullable = false, length = 100)
    private String metric;

    @Column(name = "value_numeric", precision = 24, scale = 6)
    private BigDecimal valueNumeric;

    @Column(name = "value_text", length = 500)
    private String valueText;

    @Column(length = 32)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IotProtocol source;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "ingested_at", nullable = false, updatable = false)
    private Instant ingestedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (ingestedAt == null) ingestedAt = now;
        if (recordedAt == null) recordedAt = now;
        if (source == null) source = IotProtocol.MANUAL;
    }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public BigDecimal getValueNumeric() { return valueNumeric; }
    public void setValueNumeric(BigDecimal valueNumeric) { this.valueNumeric = valueNumeric; }

    public String getValueText() { return valueText; }
    public void setValueText(String valueText) { this.valueText = valueText; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public IotProtocol getSource() { return source; }
    public void setSource(IotProtocol source) { this.source = source; }

    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }

    public Instant getIngestedAt() { return ingestedAt; }
    public void setIngestedAt(Instant ingestedAt) { this.ingestedAt = ingestedAt; }
}
