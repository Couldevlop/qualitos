package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "change_requests",
        uniqueConstraints = @UniqueConstraint(name = "uk_change_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_change_tenant", columnList = "tenant_id"),
                @Index(name = "idx_change_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_change_tenant_type", columnList = "tenant_id, type")
        })
public class ChangeRequest {

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
    private ChangeRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChangeRequestPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChangeRequestStatus status;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "planned_for")
    private LocalDate plannedFor;

    @Column(name = "implemented_at")
    private LocalDate implementedAt;

    @Column(name = "impact_summary", length = 2000)
    private String impactSummary;

    @Column(name = "risk_assessment", length = 2000)
    private String riskAssessment;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ChangeRequestStatus.DRAFT;
        if (priority == null) priority = ChangeRequestPriority.MEDIUM;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

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
    public ChangeRequestType getType() { return type; }
    public void setType(ChangeRequestType type) { this.type = type; }
    public ChangeRequestPriority getPriority() { return priority; }
    public void setPriority(ChangeRequestPriority priority) { this.priority = priority; }
    public ChangeRequestStatus getStatus() { return status; }
    public void setStatus(ChangeRequestStatus status) { this.status = status; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(UUID requesterUserId) { this.requesterUserId = requesterUserId; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }
    public LocalDate getPlannedFor() { return plannedFor; }
    public void setPlannedFor(LocalDate plannedFor) { this.plannedFor = plannedFor; }
    public LocalDate getImplementedAt() { return implementedAt; }
    public void setImplementedAt(LocalDate implementedAt) { this.implementedAt = implementedAt; }
    public String getImpactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) { this.impactSummary = impactSummary; }
    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
