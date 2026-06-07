package com.openlab.qualitos.quality.ehrconnector;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'idempotence : une ressource FHIR déjà importée → la NC créée.
 *
 * <p>Garantit qu'une 2e synchronisation ne recrée pas de NC pour une ressource
 * déjà traitée (clé unique {@code connection_id + fhir_resource_id}).</p>
 *
 * <p>PRIVACY (§11.3) : on ne conserve QUE l'identifiant TECHNIQUE de la ressource
 * FHIR ({@link #fhirResourceId}) et son type — aucun identifiant patient nominatif,
 * aucune donnée clinique en clair.</p>
 */
@Entity
@Table(name = "ehr_imported_resources",
        uniqueConstraints = @UniqueConstraint(name = "uk_ehr_imported_resource",
                columnNames = {"connection_id", "fhir_resource_id"}))
public class EhrImportedResource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "fhir_resource_type", nullable = false, length = 64)
    private String fhirResourceType;

    @Column(name = "fhir_resource_id", nullable = false, length = 256)
    private String fhirResourceId;

    @Column(name = "nc_id", nullable = false)
    private UUID ncId;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @PrePersist
    void prePersist() {
        if (importedAt == null) importedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = connectionId; }

    public String getFhirResourceType() { return fhirResourceType; }
    public void setFhirResourceType(String fhirResourceType) { this.fhirResourceType = fhirResourceType; }

    public String getFhirResourceId() { return fhirResourceId; }
    public void setFhirResourceId(String fhirResourceId) { this.fhirResourceId = fhirResourceId; }

    public UUID getNcId() { return ncId; }
    public void setNcId(UUID ncId) { this.ncId = ncId; }

    public Instant getImportedAt() { return importedAt; }
    public void setImportedAt(Instant importedAt) { this.importedAt = importedAt; }
}
