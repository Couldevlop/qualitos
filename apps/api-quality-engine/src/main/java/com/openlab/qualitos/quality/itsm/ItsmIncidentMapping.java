package com.openlab.qualitos.quality.itsm;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapping incident externe ↔ entité QualitOS locale.
 *
 * Garantit l'idempotence des imports : un incident ServiceNow déjà importé ne sera
 * jamais re-créé en double, même si le sync est ré-exécuté.
 *
 * Le champ {@code internal_entity_type} indique vers quel domaine l'incident a été
 * mappé (NON_CONFORMITY pour V1 ; CAPA pour V2 quand on rebranche le flow CAPA).
 */
@Entity
@Table(name = "itsm_incident_mappings",
        uniqueConstraints = @UniqueConstraint(name = "uk_itsm_map_external",
                columnNames = {"connection_id", "external_id"}),
        indexes = {
                @Index(name = "idx_itsm_map_tenant", columnList = "tenant_id"),
                @Index(name = "idx_itsm_map_conn", columnList = "connection_id")
        })
public class ItsmIncidentMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "external_url", length = 1024)
    private String externalUrl;

    @Column(name = "external_status", length = 64)
    private String externalStatus;

    @Column(name = "external_priority", length = 32)
    private String externalPriority;

    @Column(name = "external_title", length = 500)
    private String externalTitle;

    /** Type d'entité locale créée (NON_CONFORMITY pour V1). */
    @Column(name = "internal_entity_type", nullable = false, length = 64)
    private String internalEntityType;

    @Column(name = "internal_entity_id")
    private UUID internalEntityId;

    @Column(name = "first_imported_at", nullable = false, updatable = false)
    private Instant firstImportedAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (firstImportedAt == null) firstImportedAt = now;
        if (lastSeenAt == null) lastSeenAt = now;
    }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public UUID getConnectionId() { return connectionId; }
    public void setConnectionId(UUID connectionId) { this.connectionId = connectionId; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    public String getExternalStatus() { return externalStatus; }
    public void setExternalStatus(String externalStatus) { this.externalStatus = externalStatus; }

    public String getExternalPriority() { return externalPriority; }
    public void setExternalPriority(String externalPriority) { this.externalPriority = externalPriority; }

    public String getExternalTitle() { return externalTitle; }
    public void setExternalTitle(String externalTitle) { this.externalTitle = externalTitle; }

    public String getInternalEntityType() { return internalEntityType; }
    public void setInternalEntityType(String internalEntityType) { this.internalEntityType = internalEntityType; }

    public UUID getInternalEntityId() { return internalEntityId; }
    public void setInternalEntityId(UUID internalEntityId) { this.internalEntityId = internalEntityId; }

    public Instant getFirstImportedAt() { return firstImportedAt; }
    public void setFirstImportedAt(Instant firstImportedAt) { this.firstImportedAt = firstImportedAt; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
