package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.ImpactLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** L'évaluation d'un domaine d'impact d'une demande : un niveau, la nature de l'impact et ses mesures. */
@Entity
@Table(name = "change_impact_assessments",
        uniqueConstraints = @UniqueConstraint(name = "uq_change_impact_domain", columnNames = {"change_id", "domain"}))
public class ChangeImpactAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "change_id", nullable = false)
    private UUID changeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImpactDomain domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ImpactLevel level;

    @Column(length = 2000)
    private String notes;

    @Column(name = "assessed_by", nullable = false)
    private UUID assessedBy;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getChangeId() { return changeId; }
    public void setChangeId(UUID changeId) { this.changeId = changeId; }
    public ImpactDomain getDomain() { return domain; }
    public void setDomain(ImpactDomain domain) { this.domain = domain; }
    public ImpactLevel getLevel() { return level; }
    public void setLevel(ImpactLevel level) { this.level = level; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getAssessedBy() { return assessedBy; }
    public void setAssessedBy(UUID assessedBy) { this.assessedBy = assessedBy; }
    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
}
