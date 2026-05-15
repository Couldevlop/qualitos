package com.openlab.qualitos.quality.complaints;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Réclamation client (§4.9). Le {@link #satisfactionScore} suit l'échelle 0..10
 * type NPS — la sentiment-analyse NLP arrivera par le futur AI service (§12).
 */
@Entity
@Table(name = "complaints",
        uniqueConstraints = @UniqueConstraint(name = "uk_complaint_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_complaint_tenant", columnList = "tenant_id"),
                @Index(name = "idx_complaint_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_complaint_tenant_category", columnList = "tenant_id, category"),
                @Index(name = "idx_complaint_tenant_supplier", columnList = "tenant_id, supplier_id")
        })
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ComplaintChannel channel;

    @Column(name = "customer_name", length = 250)
    private String customerName;

    @Column(name = "customer_email", length = 320)
    private String customerEmail;

    /** Identifiant client externe (n° de commande, compte CRM, sticker IoT…). */
    @Column(name = "customer_external_id", length = 200)
    private String customerExternalId;

    @Column(nullable = false, length = 250)
    private String subject;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ComplaintSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ComplaintStatus status;

    /** Lien optionnel vers un fournisseur (cas d'usage supplier QM). */
    @Column(name = "supplier_id")
    private UUID supplierId;

    /** Lien optionnel vers un cas CAPA si une action est ouverte. */
    @Column(name = "capa_case_id")
    private UUID capaCaseId;

    @Column(name = "assigned_to_user_id")
    private UUID assignedToUserId;

    /** Score 0..10 type NPS. Null tant que pas de retour client. */
    @Column(name = "satisfaction_score")
    private Integer satisfactionScore;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "first_response_at")
    private Instant firstResponseAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

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
        if (status == null) status = ComplaintStatus.RECEIVED;
        if (severity == null) severity = ComplaintSeverity.MEDIUM;
        if (category == null) category = ComplaintCategory.OTHER;
        if (receivedAt == null) receivedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public ComplaintChannel getChannel() { return channel; }
    public void setChannel(ComplaintChannel channel) { this.channel = channel; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerExternalId() { return customerExternalId; }
    public void setCustomerExternalId(String customerExternalId) { this.customerExternalId = customerExternalId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ComplaintSeverity getSeverity() { return severity; }
    public void setSeverity(ComplaintSeverity severity) { this.severity = severity; }
    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public UUID getCapaCaseId() { return capaCaseId; }
    public void setCapaCaseId(UUID capaCaseId) { this.capaCaseId = capaCaseId; }
    public UUID getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(UUID assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public Integer getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Integer satisfactionScore) { this.satisfactionScore = satisfactionScore; }
    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
    public Instant getFirstResponseAt() { return firstResponseAt; }
    public void setFirstResponseAt(Instant firstResponseAt) { this.firstResponseAt = firstResponseAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
