package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.MocClassification;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Une demande de changement (MOC, ADR 0087).
 *
 * <p>La table garde son nom et ses identifiants d'avant le MOC : le registre
 * des risques cite des changements comme origine, et le tableau de bord SMI en
 * relit les échéances.
 *
 * <p>{@code impactScore} et {@code classification} sont recalculés tant que la
 * demande est en brouillon ou en analyse, puis FIGÉS à la soumission : un
 * réglage changé ensuite ne reclasse pas une demande déjà devant ses validateurs.
 * {@code impactSummary} est la synthèse texte de l'analyse (natures d'impact
 * des domaines évalués), reprise comme « effet » d'un risque créé depuis la demande.
 */
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

    @Column(length = 4000)
    private String justification;

    @Column(length = 200)
    private String site;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChangeRequestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ChangeRequestStatus status;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "requester_name", length = 200)
    private String requesterName;

    @Column(name = "planned_for")
    private LocalDate plannedFor;

    @Column(name = "impact_score")
    private Integer impactScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MocClassification classification;

    @Column(name = "impact_summary", length = 2000)
    private String impactSummary;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    /** Deux validateurs qui statuent au même instant ne concluent pas la demande deux fois. */
    @Version
    @Column(nullable = false)
    private long version;

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
        if (type == null) type = ChangeRequestType.PERMANENT;
        if (classification == null) classification = MocClassification.NOT_ASSESSED;
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
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public ChangeRequestType getType() { return type; }
    public void setType(ChangeRequestType type) { this.type = type; }
    public ChangeRequestStatus getStatus() { return status; }
    public void setStatus(ChangeRequestStatus status) { this.status = status; }
    public UUID getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(UUID requesterUserId) { this.requesterUserId = requesterUserId; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public LocalDate getPlannedFor() { return plannedFor; }
    public void setPlannedFor(LocalDate plannedFor) { this.plannedFor = plannedFor; }
    public Integer getImpactScore() { return impactScore; }
    public void setImpactScore(Integer impactScore) { this.impactScore = impactScore; }
    public MocClassification getClassification() { return classification; }
    public void setClassification(MocClassification classification) { this.classification = classification; }
    public String getImpactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) { this.impactSummary = impactSummary; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
