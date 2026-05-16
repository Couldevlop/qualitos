package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gdpr_privacy_notices",
        indexes = {
                @Index(name = "idx_pn_tenant", columnList = "tenant_id"),
                @Index(name = "idx_pn_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_pn_tenant_reference_language",
                        columnList = "tenant_id, reference, language"),
                @Index(name = "uq_pn_tenant_ref_version_lang",
                        columnList = "tenant_id, reference, version, language", unique = true)
        })
public class PrivacyNoticeJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String reference;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(nullable = false, length = 2)
    private String language;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 2000)
    private String summary;

    @Column(name = "content_markdown", length = 65000)
    private String contentMarkdown;

    @Column(name = "linked_processing_activity_ids", length = 4000)
    private String linkedProcessingActivityIdsCsv;

    @Column(name = "publish_url", length = 1024)
    private String publishUrl;

    @Column(name = "contact_name", length = 250)
    private String contactName;

    @Column(name = "contact_email", length = 250)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PrivacyNoticeStatus status;

    @Column(name = "effective_from")
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private UUID publishedByUserId;

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
    public String getVersion() { return version; }
    public void setVersion(String v) { this.version = v; }
    public String getLanguage() { return language; }
    public void setLanguage(String v) { this.language = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getSummary() { return summary; }
    public void setSummary(String v) { this.summary = v; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String v) { this.contentMarkdown = v; }
    public String getLinkedProcessingActivityIdsCsv() { return linkedProcessingActivityIdsCsv; }
    public void setLinkedProcessingActivityIdsCsv(String v) { this.linkedProcessingActivityIdsCsv = v; }
    public String getPublishUrl() { return publishUrl; }
    public void setPublishUrl(String v) { this.publishUrl = v; }
    public String getContactName() { return contactName; }
    public void setContactName(String v) { this.contactName = v; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String v) { this.contactEmail = v; }
    public PrivacyNoticeStatus getStatus() { return status; }
    public void setStatus(PrivacyNoticeStatus v) { this.status = v; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Instant v) { this.effectiveFrom = v; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Instant v) { this.effectiveTo = v; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant v) { this.publishedAt = v; }
    public UUID getPublishedByUserId() { return publishedByUserId; }
    public void setPublishedByUserId(UUID v) { this.publishedByUserId = v; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID v) { this.createdByUserId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
