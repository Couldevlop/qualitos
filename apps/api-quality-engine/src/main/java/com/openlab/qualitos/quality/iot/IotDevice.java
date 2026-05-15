package com.openlab.qualitos.quality.iot;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation logique d'un équipement IoT (CLAUDE.md §9.6).
 *
 * Le couple (tenant_id, code) est unique — un code humain stable type
 * "machine-A-line-3" est ce que les opérateurs reconnaissent. Le champ
 * {@link #location} suit la hiérarchie ISA-95 par convention CSV
 * "site|area|line|cell" mais reste libre.
 */
@Entity
@Table(name = "iot_devices",
        uniqueConstraints = @UniqueConstraint(name = "uk_iot_device_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_iot_device_tenant", columnList = "tenant_id"),
                @Index(name = "idx_iot_device_status", columnList = "tenant_id, status"),
                @Index(name = "idx_iot_device_type", columnList = "tenant_id, device_type")
        })
public class IotDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 32)
    private IotDeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IotProtocol protocol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IotDeviceStatus status;

    /** Hiérarchie ISA-95 "site|area|line|cell" ou libre. */
    @Column(length = 500)
    private String location;

    /** Description libre. */
    @Column(length = 1000)
    private String description;

    /** Métadonnées custom JSON. */
    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "telemetry_count", nullable = false)
    private long telemetryCount;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = IotDeviceStatus.PROVISIONED;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public IotDeviceType getDeviceType() { return deviceType; }
    public void setDeviceType(IotDeviceType deviceType) { this.deviceType = deviceType; }

    public IotProtocol getProtocol() { return protocol; }
    public void setProtocol(IotProtocol protocol) { this.protocol = protocol; }

    public IotDeviceStatus getStatus() { return status; }
    public void setStatus(IotDeviceStatus status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public long getTelemetryCount() { return telemetryCount; }
    public void setTelemetryCount(long telemetryCount) { this.telemetryCount = telemetryCount; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
