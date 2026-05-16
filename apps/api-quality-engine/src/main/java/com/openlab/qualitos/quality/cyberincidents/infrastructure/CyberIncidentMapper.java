package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

final class CyberIncidentMapper {
    private CyberIncidentMapper() {}

    static CyberIncidentJpaEntity toEntity(CyberIncident i, CyberIncidentJpaEntity target) {
        CyberIncidentJpaEntity e = target != null ? target : new CyberIncidentJpaEntity();
        if (i.getId() != null) e.setId(i.getId());
        e.setTenantId(i.getTenantId());
        e.setReference(i.getReference());
        e.setTitle(i.getTitle());
        e.setDescription(i.getDescription());
        e.setDetectedAt(i.getDetectedAt());
        e.setOccurredAt(i.getOccurredAt());
        e.setEarlyWarningDeadlineAt(i.getEarlyWarningDeadlineAt());
        e.setInitialAssessmentDeadlineAt(i.getInitialAssessmentDeadlineAt());
        e.setFinalReportDeadlineAt(i.getFinalReportDeadlineAt());
        e.setIncidentType(i.getIncidentType());
        e.setSeverity(i.getSeverity());
        e.setStatus(i.getStatus());
        e.setEstimatedAffectedUsers(i.getEstimatedAffectedUsers());
        e.setAffectedAssetsCsv(toCsv(i.getAffectedAssets()));
        e.setAffectedServicesCsv(toCsv(i.getAffectedServices()));
        e.setLinkedBreachId(i.getLinkedBreachId());
        e.setContainmentMeasures(i.getContainmentMeasures());
        e.setImpactDescription(i.getImpactDescription());
        e.setEarlyWarningSentAt(i.getEarlyWarningSentAt());
        e.setEarlyWarningReference(i.getEarlyWarningReference());
        e.setInitialAssessmentSentAt(i.getInitialAssessmentSentAt());
        e.setInitialAssessmentReference(i.getInitialAssessmentReference());
        e.setFinalReportSentAt(i.getFinalReportSentAt());
        e.setFinalReportReference(i.getFinalReportReference());
        e.setClosureNotes(i.getClosureNotes());
        e.setRejectionReason(i.getRejectionReason());
        e.setReportedByUserId(i.getReportedByUserId());
        e.setHandledByUserId(i.getHandledByUserId());
        e.setClosedAt(i.getClosedAt());
        e.setUpdatedAt(i.getUpdatedAt());
        return e;
    }

    static CyberIncident toDomain(CyberIncidentJpaEntity e) {
        return new CyberIncident(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getTitle(), e.getDescription(),
                e.getDetectedAt(), e.getOccurredAt(),
                e.getEarlyWarningDeadlineAt(), e.getInitialAssessmentDeadlineAt(),
                e.getFinalReportDeadlineAt(),
                e.getIncidentType(), e.getSeverity(), e.getStatus(),
                e.getEstimatedAffectedUsers(),
                fromCsv(e.getAffectedAssetsCsv()), fromCsv(e.getAffectedServicesCsv()),
                e.getLinkedBreachId(),
                e.getContainmentMeasures(), e.getImpactDescription(),
                e.getEarlyWarningSentAt(), e.getEarlyWarningReference(),
                e.getInitialAssessmentSentAt(), e.getInitialAssessmentReference(),
                e.getFinalReportSentAt(), e.getFinalReportReference(),
                e.getClosureNotes(), e.getRejectionReason(),
                e.getReportedByUserId(), e.getHandledByUserId(),
                e.getClosedAt(), e.getUpdatedAt());
    }

    private static String toCsv(Set<String> s) {
        if (s == null || s.isEmpty()) return null;
        return String.join(",", s);
    }

    private static Set<String> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
