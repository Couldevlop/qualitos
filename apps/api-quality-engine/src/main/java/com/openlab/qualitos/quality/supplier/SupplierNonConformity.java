package com.openlab.qualitos.quality.supplier;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Non-conformité fournisseur : un défaut matière, lot, prestation.
 * Distinct du module NC global pour permettre la corrélation directe au supplier
 * et le calcul de score.
 */
@Entity
@Table(name = "supplier_non_conformities",
        indexes = {
                @Index(name = "idx_supplier_nc_supplier",
                        columnList = "supplier_id, detected_on"),
                @Index(name = "idx_supplier_nc_tenant_status",
                        columnList = "tenant_id, status")
        })
public class SupplierNonConformity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "lot_reference", length = 100)
    private String lotReference;

    @Column(length = 1000)
    private String description;

    /** Référence de la pièce touchée. Exigée à la saisie ; absente des NC antérieures (V150). */
    @Column(name = "part_number", length = 100)
    private String partNumber;

    /** Nombre de pièces touchées, strictement positif. Même régime que {@link #partNumber}. */
    @Column(name = "part_quantity")
    private Integer partQuantity;

    /** Un rapport 8D a été demandé au fournisseur. */
    @Column(name = "eight_d_requested", nullable = false)
    private boolean eightDRequested;

    /** Le fournisseur a remis son rapport 8D. */
    @Column(name = "eight_d_received", nullable = false)
    private boolean eightDReceived;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NonConformitySeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NonConformityStatus status;

    @Column(name = "detected_on", nullable = false)
    private LocalDate detectedOn;

    @Column(name = "resolved_on")
    private LocalDate resolvedOn;

    @Column(name = "resolution", length = 1000)
    private String resolution;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = NonConformityStatus.OPEN;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public String getLotReference() { return lotReference; }
    public void setLotReference(String lotReference) { this.lotReference = lotReference; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public Integer getPartQuantity() { return partQuantity; }
    public void setPartQuantity(Integer partQuantity) { this.partQuantity = partQuantity; }
    public boolean isEightDRequested() { return eightDRequested; }
    public void setEightDRequested(boolean eightDRequested) { this.eightDRequested = eightDRequested; }
    public boolean isEightDReceived() { return eightDReceived; }
    public void setEightDReceived(boolean eightDReceived) { this.eightDReceived = eightDReceived; }
    public NonConformitySeverity getSeverity() { return severity; }
    public void setSeverity(NonConformitySeverity severity) { this.severity = severity; }
    public NonConformityStatus getStatus() { return status; }
    public void setStatus(NonConformityStatus status) { this.status = status; }
    public LocalDate getDetectedOn() { return detectedOn; }
    public void setDetectedOn(LocalDate detectedOn) { this.detectedOn = detectedOn; }
    public LocalDate getResolvedOn() { return resolvedOn; }
    public void setResolvedOn(LocalDate resolvedOn) { this.resolvedOn = resolvedOn; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
