package com.openlab.qualitos.quality.breach.infrastructure;

import com.openlab.qualitos.quality.breach.domain.BreachIncident;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class BreachMapper {
    private BreachMapper() {}

    static BreachJpaEntity toEntity(BreachIncident i, BreachJpaEntity target) {
        BreachJpaEntity e = target != null ? target : new BreachJpaEntity();
        if (i.getId() != null) e.setId(i.getId());
        e.setTenantId(i.getTenantId());
        e.setInternalReference(i.getInternalReference());
        e.setTitle(i.getTitle());
        e.setDescription(i.getDescription());
        e.setDetectedAt(i.getDetectedAt());
        e.setOccurredAt(i.getOccurredAt());
        e.setDpaDeadlineAt(i.getDpaDeadlineAt());
        e.setSeverity(i.getSeverity());
        e.setStatus(i.getStatus());
        e.setAffectedSubjectsCount(i.getAffectedSubjectsCount());
        e.setAffectedDataCategoriesCsv(toCsv(i.getAffectedDataCategories()));
        e.setRiskOfHarmDescription(i.getRiskOfHarmDescription());
        e.setContainmentMeasures(i.getContainmentMeasures());
        e.setDpaNotifiedAt(i.getDpaNotifiedAt());
        e.setDpaReference(i.getDpaReference());
        e.setSubjectsNotifiedAt(i.getSubjectsNotifiedAt());
        e.setSubjectsNotificationChannel(i.getSubjectsNotificationChannel());
        e.setRejectionReason(i.getRejectionReason());
        e.setClosureNotes(i.getClosureNotes());
        e.setReportedByUserId(i.getReportedByUserId());
        e.setHandledByUserId(i.getHandledByUserId());
        e.setClosedAt(i.getClosedAt());
        e.setUpdatedAt(i.getUpdatedAt());
        return e;
    }

    static BreachIncident toDomain(BreachJpaEntity e) {
        return new BreachIncident(
                e.getId(), e.getTenantId(),
                e.getInternalReference(), e.getTitle(), e.getDescription(),
                e.getDetectedAt(), e.getOccurredAt(), e.getDpaDeadlineAt(),
                e.getSeverity(), e.getStatus(),
                e.getAffectedSubjectsCount(),
                fromCsv(e.getAffectedDataCategoriesCsv()),
                e.getRiskOfHarmDescription(), e.getContainmentMeasures(),
                e.getDpaNotifiedAt(), e.getDpaReference(),
                e.getSubjectsNotifiedAt(), e.getSubjectsNotificationChannel(),
                e.getRejectionReason(), e.getClosureNotes(),
                e.getReportedByUserId(), e.getHandledByUserId(),
                e.getClosedAt(), e.getUpdatedAt());
    }

    private static String toCsv(Set<String> s) {
        if (s == null || s.isEmpty()) return null;
        return s.stream().collect(Collectors.joining(","));
    }

    private static Set<String> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
