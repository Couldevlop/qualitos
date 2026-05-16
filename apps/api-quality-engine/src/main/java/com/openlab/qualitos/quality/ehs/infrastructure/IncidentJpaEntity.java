package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStatus;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Représentation JPA — strictement infrastructure. L'agrégat domaine
 * {@code Incident} ne dépend pas de cette classe.
 */
@Entity
@Table(name = "ehs_incidents",
        uniqueConstraints = @UniqueConstraint(name = "uk_ehs_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_ehs_tenant", columnList = "tenant_id"),
                @Index(name = "idx_ehs_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_ehs_tenant_type", columnList = "tenant_id, type"),
                @Index(name = "idx_ehs_tenant_severity", columnList = "tenant_id, severity")
        })
public class IncidentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reported_at", nullable = false, updatable = false)
    private Instant reportedAt;

    @Column(name = "mitigated_at")
    private Instant mitigatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(length = 500)
    private String location;

    @Column(name = "persons_involved", length = 1000)
    private String personsInvolved;

    @Column(name = "root_cause", length = 2000)
    private String rootCause;

    @Column(name = "corrective_actions", length = 2000)
    private String correctiveActions;

    @Column(name = "standards_csv", length = 500)
    private String standardsCsv;

    @Column(name = "capa_case_id")
    private UUID capaCaseId;

    @Column(name = "nc_id")
    private UUID ncId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "reported_by", nullable = false)
    private UUID reportedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public IncidentType getType() { return type; }
    public void setType(IncidentType type) { this.type = type; }
    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getReportedAt() { return reportedAt; }
    public void setReportedAt(Instant reportedAt) { this.reportedAt = reportedAt; }
    public Instant getMitigatedAt() { return mitigatedAt; }
    public void setMitigatedAt(Instant mitigatedAt) { this.mitigatedAt = mitigatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPersonsInvolved() { return personsInvolved; }
    public void setPersonsInvolved(String personsInvolved) { this.personsInvolved = personsInvolved; }
    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }
    public String getCorrectiveActions() { return correctiveActions; }
    public void setCorrectiveActions(String correctiveActions) { this.correctiveActions = correctiveActions; }
    public String getStandardsCsv() { return standardsCsv; }
    public void setStandardsCsv(String standardsCsv) { this.standardsCsv = standardsCsv; }
    public UUID getCapaCaseId() { return capaCaseId; }
    public void setCapaCaseId(UUID capaCaseId) { this.capaCaseId = capaCaseId; }
    public UUID getNcId() { return ncId; }
    public void setNcId(UUID ncId) { this.ncId = ncId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public UUID getReportedBy() { return reportedBy; }
    public void setReportedBy(UUID reportedBy) { this.reportedBy = reportedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
