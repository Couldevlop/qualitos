package com.openlab.qualitos.quality.privacynotices.infrastructure;

import com.openlab.qualitos.quality.privacynotices.domain.PrivacyNotice;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class PrivacyNoticeMapper {
    private PrivacyNoticeMapper() {}

    static PrivacyNoticeJpaEntity toEntity(PrivacyNotice n, PrivacyNoticeJpaEntity target) {
        PrivacyNoticeJpaEntity e = target != null ? target : new PrivacyNoticeJpaEntity();
        if (n.getId() != null) e.setId(n.getId());
        e.setTenantId(n.getTenantId());
        e.setReference(n.getReference());
        e.setVersion(n.getVersion());
        e.setLanguage(n.getLanguage());
        e.setTitle(n.getTitle());
        e.setSummary(n.getSummary());
        e.setContentMarkdown(n.getContentMarkdown());
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(n.getLinkedProcessingActivityIds()));
        e.setPublishUrl(n.getPublishUrl());
        e.setContactName(n.getContactName());
        e.setContactEmail(n.getContactEmail());
        e.setStatus(n.getStatus());
        e.setEffectiveFrom(n.getEffectiveFrom());
        e.setEffectiveTo(n.getEffectiveTo());
        e.setPublishedAt(n.getPublishedAt());
        e.setPublishedByUserId(n.getPublishedByUserId());
        e.setCreatedByUserId(n.getCreatedByUserId());
        e.setCreatedAt(n.getCreatedAt());
        e.setUpdatedAt(n.getUpdatedAt());
        return e;
    }

    static PrivacyNotice toDomain(PrivacyNoticeJpaEntity e) {
        return new PrivacyNotice(
                e.getId(), e.getTenantId(),
                e.getReference(), e.getVersion(), e.getLanguage(),
                e.getTitle(), e.getSummary(), e.getContentMarkdown(),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                e.getPublishUrl(), e.getContactName(), e.getContactEmail(),
                e.getStatus(), e.getEffectiveFrom(), e.getEffectiveTo(),
                e.getPublishedAt(), e.getPublishedByUserId(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String uuidSetToCsv(Set<UUID> s) {
        if (s == null || s.isEmpty()) return null;
        return s.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    private static Set<UUID> csvToUuidSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
