package com.openlab.qualitos.quality.calibration;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Équipement de mesure soumis à calibration (§4.10).
 *
 * Le champ {@link #iotDeviceId} référence éventuellement un device du module IoT
 * Hub — pas une FK dure pour éviter le couplage circulaire entre modules ; la
 * cohérence reste applicative.
 *
 * Le drapeau {@link #critical} active une politique de blocage : un équipement
 * critique en CALIBRATION FAIL ne peut pas être utilisé tant qu'un nouveau record
 * PASS n'a pas été enregistré.
 */
@Entity
@Table(name = "calibration_equipments",
        uniqueConstraints = @UniqueConstraint(name = "uk_calibration_equipment_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_calibration_equipment_tenant", columnList = "tenant_id"),
                @Index(name = "idx_calibration_equipment_status", columnList = "tenant_id, status"),
                @Index(name = "idx_calibration_equipment_iot", columnList = "iot_device_id")
        })
public class CalibrationEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 200)
    private String manufacturer;

    @Column(length = 200)
    private String model;

    @Column(name = "serial_number", length = 200)
    private String serialNumber;

    @Column(length = 500)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EquipmentStatus status;

    @Column(name = "critical", nullable = false)
    private boolean critical;

    @Column(name = "iot_device_id")
    private UUID iotDeviceId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

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
        if (status == null) status = EquipmentStatus.ACTIVE;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
    public boolean isCritical() { return critical; }
    public void setCritical(boolean critical) { this.critical = critical; }
    public UUID getIotDeviceId() { return iotDeviceId; }
    public void setIotDeviceId(UUID iotDeviceId) { this.iotDeviceId = iotDeviceId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
