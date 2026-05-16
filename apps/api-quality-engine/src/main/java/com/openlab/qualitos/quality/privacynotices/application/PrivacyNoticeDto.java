package com.openlab.qualitos.quality.privacynotices.application;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;
import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNoticeStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class PrivacyNoticeDto {

    private PrivacyNoticeDto() {}

    public record CreateRequest(
            String reference,
            String version,
            String language,
            String title,
            String summary,
            String contentMarkdown,
            Set<UUID> linkedProcessingActivityIds,
            String publishUrl,
            String contactName,
            String contactEmail,
            UUID createdByUserId) {}

    public record EditRequest(
            String title,
            String summary,
            String contentMarkdown,
            Set<UUID> linkedProcessingActivityIds,
            String publishUrl,
            String contactName,
            String contactEmail) {}

    public record PublishRequest(UUID publishedByUserId) {}

    public record View(
            UUID id, UUID tenantId,
            String reference, String version, String language,
            String title, String summary, String contentMarkdown,
            Set<UUID> linkedProcessingActivityIds,
            String publishUrl, String contactName, String contactEmail,
            PrivacyNoticeStatus status,
            Instant effectiveFrom, Instant effectiveTo,
            Instant publishedAt, UUID publishedByUserId,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(PrivacyNotice n) {
            return new View(
                    n.getId(), n.getTenantId(),
                    n.getReference(), n.getVersion(), n.getLanguage(),
                    n.getTitle(), n.getSummary(), n.getContentMarkdown(),
                    n.getLinkedProcessingActivityIds(),
                    n.getPublishUrl(), n.getContactName(), n.getContactEmail(),
                    n.getStatus(), n.getEffectiveFrom(), n.getEffectiveTo(),
                    n.getPublishedAt(), n.getPublishedByUserId(),
                    n.getCreatedByUserId(), n.getCreatedAt(), n.getUpdatedAt());
        }
    }
}
