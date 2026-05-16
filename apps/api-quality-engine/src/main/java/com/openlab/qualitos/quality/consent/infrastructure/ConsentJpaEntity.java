package com.openlab.qualitos.quality.consent.infrastructure;

import com.openlab.qualitos.quality.consent.domain.ConsentSource;
import com.openlab.qualitos.quality.consent.domain.ConsentStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_consents",
        indexes = {
                @Index(name = "idx_consent_tenant", columnList = "tenant_id"),
                @Index(name = "idx_consent_tenant_subj_purpose",
                        columnList = "tenant_id, subject_identifier_hash, purpose_code"),
                @Index(name = "idx_consent_tenant_purpose",
                        columnList = "tenant_id, purpose_code"),
                @Index(name = "idx_consent_expires", columnList = "expires_at")
        })
public class ConsentJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "subject_identifier_hash", nullable = false, length = 64)
    private String subjectIdentifierHash;

    @Column(name = "subject_identifier_label", length = 250)
    private String subjectIdentifierLabel;

    @Column(name = "purpose_code", nullable = false, length = 64)
    private String purposeCode;

    @Column(name = "purpose_version", nullable = false, length = 32)
    private String purposeVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConsentSource source;

    @Column(name = "evidence_url", length = 1024)
    private String evidenceUrl;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "granted_by")
    private UUID grantedByUserId;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConsentStatus status;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "withdrawn_by")
    private UUID withdrawnByUserId;

    @Column(name = "withdrawal_reason", length = 2000)
    private String withdrawalReason;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public String getSubjectIdentifierHash() { return subjectIdentifierHash; }
    public void setSubjectIdentifierHash(String v) { this.subjectIdentifierHash = v; }
    public String getSubjectIdentifierLabel() { return subjectIdentifierLabel; }
    public void setSubjectIdentifierLabel(String v) { this.subjectIdentifierLabel = v; }
    public String getPurposeCode() { return purposeCode; }
    public void setPurposeCode(String v) { this.purposeCode = v; }
    public String getPurposeVersion() { return purposeVersion; }
    public void setPurposeVersion(String v) { this.purposeVersion = v; }
    public ConsentSource getSource() { return source; }
    public void setSource(ConsentSource v) { this.source = v; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public void setEvidenceUrl(String v) { this.evidenceUrl = v; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String v) { this.ipAddress = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public UUID getGrantedByUserId() { return grantedByUserId; }
    public void setGrantedByUserId(UUID v) { this.grantedByUserId = v; }
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant v) { this.grantedAt = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public ConsentStatus getStatus() { return status; }
    public void setStatus(ConsentStatus v) { this.status = v; }
    public Instant getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(Instant v) { this.withdrawnAt = v; }
    public UUID getWithdrawnByUserId() { return withdrawnByUserId; }
    public void setWithdrawnByUserId(UUID v) { this.withdrawnByUserId = v; }
    public String getWithdrawalReason() { return withdrawalReason; }
    public void setWithdrawalReason(String v) { this.withdrawalReason = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
