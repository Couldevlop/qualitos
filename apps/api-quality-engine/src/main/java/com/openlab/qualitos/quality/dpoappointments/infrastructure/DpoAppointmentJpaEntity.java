package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;
import com.openlab.qualitos.quality.dpoappointments.domain.DpoType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_dpo_appointments",
        indexes = {
                @Index(name = "idx_dpo_tenant", columnList = "tenant_id"),
                @Index(name = "idx_dpo_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_dpo_tenant_scope", columnList = "tenant_id, scope"),
                @Index(name = "uq_dpo_tenant_reference",
                        columnList = "tenant_id, reference", unique = true)
        })
public class DpoAppointmentJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(name = "dpo_full_name", nullable = false, length = 250)
    private String dpoFullName;

    @Column(name = "dpo_email", nullable = false, length = 320)
    private String dpoEmail;

    @Column(name = "dpo_phone", length = 64)
    private String dpoPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "dpo_type", nullable = false, length = 32)
    private DpoType dpoType;

    @Column(name = "external_company_name", length = 250)
    private String externalCompanyName;

    @Column(length = 4000)
    private String qualifications;

    @Column(nullable = false, length = 64)
    private String scope;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "regulator_notified_at")
    private Instant regulatorNotifiedAt;

    @Column(name = "regulator_notification_reference", length = 250)
    private String regulatorNotificationReference;

    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DpoAppointmentStatus status;

    @Column(name = "end_reason", length = 2000)
    private String endReason;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getReference() { return reference; }
    public void setReference(String v) { this.reference = v; }
    public String getDpoFullName() { return dpoFullName; }
    public void setDpoFullName(String v) { this.dpoFullName = v; }
    public String getDpoEmail() { return dpoEmail; }
    public void setDpoEmail(String v) { this.dpoEmail = v; }
    public String getDpoPhone() { return dpoPhone; }
    public void setDpoPhone(String v) { this.dpoPhone = v; }
    public DpoType getDpoType() { return dpoType; }
    public void setDpoType(DpoType v) { this.dpoType = v; }
    public String getExternalCompanyName() { return externalCompanyName; }
    public void setExternalCompanyName(String v) { this.externalCompanyName = v; }
    public String getQualifications() { return qualifications; }
    public void setQualifications(String v) { this.qualifications = v; }
    public String getScope() { return scope; }
    public void setScope(String v) { this.scope = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public Instant getRegulatorNotifiedAt() { return regulatorNotifiedAt; }
    public void setRegulatorNotifiedAt(Instant v) { this.regulatorNotifiedAt = v; }
    public String getRegulatorNotificationReference() { return regulatorNotificationReference; }
    public void setRegulatorNotificationReference(String v) { this.regulatorNotificationReference = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public DpoAppointmentStatus getStatus() { return status; }
    public void setStatus(DpoAppointmentStatus v) { this.status = v; }
    public String getEndReason() { return endReason; }
    public void setEndReason(String v) { this.endReason = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
