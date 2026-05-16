package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class Nis2RiskMeasureMapper {
    private Nis2RiskMeasureMapper() {}

    static Nis2RiskMeasureJpaEntity toEntity(Nis2RiskMeasure m, Nis2RiskMeasureJpaEntity target) {
        Nis2RiskMeasureJpaEntity e = target != null ? target : new Nis2RiskMeasureJpaEntity();
        if (m.getId() != null) e.setId(m.getId());
        e.setTenantId(m.getTenantId());
        e.setReference(m.getReference());
        e.setCategory(m.getCategory());
        e.setTitle(m.getTitle());
        e.setDescription(m.getDescription());
        e.setStatus(m.getStatus());
        e.setOwnerUserId(m.getOwnerUserId());
        e.setMaturityLevel(m.getMaturityLevel());
        e.setResidualRiskRating(m.getResidualRiskRating());
        e.setCriticalRiskJustification(m.getCriticalRiskJustification());
        e.setReviewIntervalDays(m.getReviewIntervalDays());
        e.setEffectiveFrom(m.getEffectiveFrom());
        e.setEffectiveTo(m.getEffectiveTo());
        e.setLastReviewedAt(m.getLastReviewedAt());
        e.setReviewedByUserId(m.getReviewedByUserId());
        e.setNextReviewDueAt(m.getNextReviewDueAt());
        e.setEvidenceUrlsCsv(strSetToCsv(m.getEvidenceUrls()));
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(m.getLinkedProcessingActivityIds()));
        e.setLinkedProcessorAgreementIdsCsv(uuidSetToCsv(m.getLinkedProcessorAgreementIds()));
        e.setNotes(m.getNotes());
        e.setCreatedByUserId(m.getCreatedByUserId());
        e.setCreatedAt(m.getCreatedAt());
        e.setUpdatedAt(m.getUpdatedAt());
        return e;
    }

    static Nis2RiskMeasure toDomain(Nis2RiskMeasureJpaEntity e) {
        return new Nis2RiskMeasure(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getCategory(), e.getTitle(), e.getDescription(),
                e.getStatus(), e.getOwnerUserId(),
                e.getMaturityLevel(),
                e.getResidualRiskRating(), e.getCriticalRiskJustification(),
                e.getReviewIntervalDays(),
                e.getEffectiveFrom(), e.getEffectiveTo(),
                e.getLastReviewedAt(), e.getReviewedByUserId(), e.getNextReviewDueAt(),
                csvToStrSet(e.getEvidenceUrlsCsv()),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                csvToUuidSet(e.getLinkedProcessorAgreementIdsCsv()),
                e.getNotes(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String strSetToCsv(Set<String> s) {
        if (s == null || s.isEmpty()) return null;
        return String.join(",", s);
    }
    private static String uuidSetToCsv(Set<UUID> s) {
        if (s == null || s.isEmpty()) return null;
        return s.stream().map(UUID::toString).collect(Collectors.joining(","));
    }
    private static Set<String> csvToStrSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    private static Set<UUID> csvToUuidSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
