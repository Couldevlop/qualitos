package com.openlab.qualitos.quality.dpia.infrastructure;

import com.openlab.qualitos.quality.dpia.domain.Dpia;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class DpiaMapper {
    private DpiaMapper() {}

    static DpiaJpaEntity toEntity(Dpia d, DpiaJpaEntity target) {
        DpiaJpaEntity e = target != null ? target : new DpiaJpaEntity();
        if (d.getId() != null) e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setReference(d.getReference());
        e.setTitle(d.getTitle());
        e.setDescription(d.getDescription());
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(d.getLinkedProcessingActivityIds()));
        e.setNecessityAndProportionalityNotes(d.getNecessityAndProportionalityNotes());
        e.setRisksToRightsAndFreedoms(d.getRisksToRightsAndFreedoms());
        e.setMitigationMeasures(d.getMitigationMeasures());
        e.setOverallRiskLevel(d.getOverallRiskLevel());
        e.setConsultationRequired(d.isConsultationRequired());
        e.setConsultationNotes(d.getConsultationNotes());
        e.setStatus(d.getStatus());
        e.setDpoUserId(d.getDpoUserId());
        e.setDpoOpinion(d.getDpoOpinion());
        e.setDpoOpinionAt(d.getDpoOpinionAt());
        e.setEffectiveFrom(d.getEffectiveFrom());
        e.setEffectiveTo(d.getEffectiveTo());
        e.setCreatedByUserId(d.getCreatedByUserId());
        e.setHandledByUserId(d.getHandledByUserId());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    static Dpia toDomain(DpiaJpaEntity e) {
        return new Dpia(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getTitle(), e.getDescription(),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                e.getNecessityAndProportionalityNotes(),
                e.getRisksToRightsAndFreedoms(),
                e.getMitigationMeasures(),
                e.getOverallRiskLevel(),
                e.isConsultationRequired(), e.getConsultationNotes(),
                e.getStatus(),
                e.getDpoUserId(), e.getDpoOpinion(), e.getDpoOpinionAt(),
                e.getEffectiveFrom(), e.getEffectiveTo(),
                e.getCreatedByUserId(), e.getHandledByUserId(),
                e.getCreatedAt(), e.getUpdatedAt());
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
