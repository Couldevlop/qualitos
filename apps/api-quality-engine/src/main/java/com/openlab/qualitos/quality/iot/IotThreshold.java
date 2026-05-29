package com.openlab.qualitos.quality.iot;

import com.openlab.qualitos.quality.capa.CapaCriticity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Seuil de surveillance d'une métrique IoT (CLAUDE.md §9.7, §9.9).
 *
 * <p>Quand une mesure dépasse {@link #minValue}/{@link #maxValue}, l'ingestion
 * de télémétrie ouvre automatiquement une CAPA ({@code sourceType=IOT_ALERT}).
 * Le {@link #capaOwnerId} porté par le seuil devient le responsable de la CAPA
 * générée — pas de magie d'assignation implicite.
 *
 * <p>Un {@link #deviceId} {@code null} rend le seuil applicable à TOUS les
 * équipements du tenant pour la métrique (seuil « tenant-large »).
 */
@Entity
@Table(name = "iot_thresholds",
        indexes = {
                @Index(name = "idx_iot_threshold_tenant_metric", columnList = "tenant_id, metric"),
                @Index(name = "idx_iot_threshold_device", columnList = "tenant_id, device_id")
        })
public class IotThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Null = seuil applicable à tous les équipements du tenant pour la métrique. */
    @Column(name = "device_id")
    private UUID deviceId;

    @Column(nullable = false, length = 100)
    private String metric;

    /** Borne basse (incluse comme conforme) — dépassement si valeur &lt; minValue. */
    @Column(name = "min_value")
    private Double minValue;

    /** Borne haute (incluse comme conforme) — dépassement si valeur &gt; maxValue. */
    @Column(name = "max_value")
    private Double maxValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "capa_criticity", nullable = false, length = 16)
    private CapaCriticity capaCriticity;

    /** Responsable assigné à la CAPA générée par ce seuil. */
    @Column(name = "capa_owner_id", nullable = false)
    private UUID capaOwnerId;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    /** Vrai si la mesure sort des bornes configurées. */
    public boolean isBreached(double value) {
        if (minValue != null && value < minValue) return true;
        return maxValue != null && value > maxValue;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getDeviceId() { return deviceId; }
    public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }

    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }

    public Double getMinValue() { return minValue; }
    public void setMinValue(Double minValue) { this.minValue = minValue; }

    public Double getMaxValue() { return maxValue; }
    public void setMaxValue(Double maxValue) { this.maxValue = maxValue; }

    public CapaCriticity getCapaCriticity() { return capaCriticity; }
    public void setCapaCriticity(CapaCriticity capaCriticity) { this.capaCriticity = capaCriticity; }

    public UUID getCapaOwnerId() { return capaOwnerId; }
    public void setCapaOwnerId(UUID capaOwnerId) { this.capaOwnerId = capaOwnerId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
