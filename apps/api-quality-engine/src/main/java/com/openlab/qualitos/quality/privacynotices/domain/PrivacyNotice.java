package com.openlab.qualitos.quality.privacynotices.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — mention d'information aux personnes concernées (RGPD Art. 13/14).
 *
 * Une PrivacyNotice décrit le texte exact présenté aux personnes au moment
 * de la collecte (ou ultérieurement, pour Art. 14). Versionnée et
 * multilingue : un même {@code reference} peut avoir plusieurs versions
 * historisées dans plusieurs langues.
 *
 * Cycle de vie :
 *   DRAFT → PUBLISHED → ARCHIVED
 *   DRAFT → (deletable)
 *
 * Invariants :
 *  - PUBLISHED immutable (toute évolution = nouvelle version DRAFT).
 *  - {@code contentMarkdown}, {@code summary}, {@code title} obligatoires
 *    avant publication.
 *  - {@code language} ISO 639-1 alpha-2 (en/fr/es/...).
 *  - Unicité (tenant, reference, version, language) garantie par DB.
 *  - Au plus une PUBLISHED par (tenant, reference, language) — service
 *    auto-archive la précédente (garantie DB par index partiel).
 *
 * Privacy by design (OWASP A02) : aucun PII concret — le contenu est
 * destiné à un public général, et les références sont structurelles.
 */
public final class PrivacyNotice {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[A-Za-z0-9._:-]{1,32}$");
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("^[a-z]{2}$"); // ISO 639-1
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://[^\\s]{1,1022}$");

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final String version;
    private final String language;
    private String title;
    private String summary;
    private String contentMarkdown;
    private Set<UUID> linkedProcessingActivityIds;
    private String publishUrl;
    private String contactName;
    private String contactEmail;
    private PrivacyNoticeStatus status;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant publishedAt;
    private UUID publishedByUserId;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public PrivacyNotice(UUID id, UUID tenantId,
                         String reference, String version, String language,
                         String title, String summary, String contentMarkdown,
                         Set<UUID> linkedProcessingActivityIds,
                         String publishUrl, String contactName, String contactEmail,
                         PrivacyNoticeStatus status,
                         Instant effectiveFrom, Instant effectiveTo,
                         Instant publishedAt, UUID publishedByUserId,
                         UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.version = requireVersion(version);
        this.language = requireLanguage(language);
        this.title = requireText(title, "title", 250);
        this.summary = summary;
        this.contentMarkdown = contentMarkdown;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.publishUrl = sanitizeUrl(publishUrl);
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.status = Objects.requireNonNull(status, "status");
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.publishedAt = publishedAt;
        this.publishedByUserId = publishedByUserId;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    /** Factory — création d'une mention DRAFT. */
    public static PrivacyNotice draft(UUID tenantId,
                                      String reference, String version, String language,
                                      String title, String summary, String contentMarkdown,
                                      Set<UUID> linkedProcessingActivityIds,
                                      String publishUrl,
                                      String contactName, String contactEmail,
                                      UUID createdByUserId, Instant now) {
        return new PrivacyNotice(null, tenantId, reference, version, language,
                title, summary, contentMarkdown, linkedProcessingActivityIds,
                publishUrl, contactName, contactEmail,
                PrivacyNoticeStatus.DRAFT, null, null, null, null,
                createdByUserId, now, now);
    }

    /** Édition — DRAFT uniquement. {@code title} reste exigé. */
    public void editDraft(String title, String summary, String contentMarkdown,
                          Set<UUID> linkedProcessingActivityIds,
                          String publishUrl, String contactName, String contactEmail,
                          Instant now) {
        if (status != PrivacyNoticeStatus.DRAFT) {
            throw new PrivacyNoticeStateException("Only DRAFT notices can be edited");
        }
        this.title = requireText(title, "title", 250);
        this.summary = summary;
        this.contentMarkdown = contentMarkdown;
        this.linkedProcessingActivityIds = sanitizeIds(linkedProcessingActivityIds);
        this.publishUrl = sanitizeUrl(publishUrl);
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.updatedAt = now;
    }

    /** Publication — exige summary + contentMarkdown renseignés. */
    public void publish(UUID publishedByUserId, Instant now) {
        if (status != PrivacyNoticeStatus.DRAFT) {
            throw new PrivacyNoticeStateException("Only DRAFT notices can be published");
        }
        if (publishedByUserId == null) {
            throw new PrivacyNoticeStateException("publishedByUserId required");
        }
        requireText(summary, "summary", 2000);
        requireText(contentMarkdown, "contentMarkdown", 65000);
        this.publishedAt = now;
        this.publishedByUserId = publishedByUserId;
        this.effectiveFrom = now;
        this.status = PrivacyNoticeStatus.PUBLISHED;
        this.updatedAt = now;
    }

    /** Archivage — depuis PUBLISHED uniquement (terminal). */
    public void archive(Instant now) {
        if (status != PrivacyNoticeStatus.PUBLISHED) {
            throw new PrivacyNoticeStateException("Only PUBLISHED notices can be archived");
        }
        this.effectiveTo = now;
        this.status = PrivacyNoticeStatus.ARCHIVED;
        this.updatedAt = now;
    }

    public boolean isDraft()     { return status == PrivacyNoticeStatus.DRAFT; }
    public boolean isPublished() { return status == PrivacyNoticeStatus.PUBLISHED; }
    public boolean isArchived()  { return status == PrivacyNoticeStatus.ARCHIVED; }

    private static String requireReference(String v) {
        if (v == null || !REF_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "reference must match [A-Z][A-Z0-9_-]{1,63}");
        }
        return v;
    }
    private static String requireVersion(String v) {
        if (v == null || !VERSION_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "version must be 1..32 chars in [A-Za-z0-9._:-]");
        }
        return v;
    }
    private static String requireLanguage(String v) {
        if (v == null || !LANGUAGE_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "language must be ISO 639-1 alpha-2 (lowercase)");
        }
        return v;
    }
    private static String requireText(String v, String f, int maxLen) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        if (v.length() > maxLen) throw new IllegalArgumentException(
                f + " too long (max " + maxLen + ")");
        return v;
    }
    private static String sanitizeUrl(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (!URL_PATTERN.matcher(t).matches()) {
            throw new IllegalArgumentException("publishUrl must be http(s) URL ≤ 1024 chars");
        }
        return t;
    }
    private static Set<UUID> sanitizeIds(Set<UUID> input) {
        if (input == null) return Collections.emptySet();
        Set<UUID> out = new LinkedHashSet<>();
        for (UUID u : input) if (u != null) out.add(u);
        return Collections.unmodifiableSet(out);
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public String getVersion() { return version; }
    public String getLanguage() { return language; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContentMarkdown() { return contentMarkdown; }
    public Set<UUID> getLinkedProcessingActivityIds() { return linkedProcessingActivityIds; }
    public String getPublishUrl() { return publishUrl; }
    public String getContactName() { return contactName; }
    public String getContactEmail() { return contactEmail; }
    public PrivacyNoticeStatus getStatus() { return status; }
    public Instant getEffectiveFrom() { return effectiveFrom; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public Instant getPublishedAt() { return publishedAt; }
    public UUID getPublishedByUserId() { return publishedByUserId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
