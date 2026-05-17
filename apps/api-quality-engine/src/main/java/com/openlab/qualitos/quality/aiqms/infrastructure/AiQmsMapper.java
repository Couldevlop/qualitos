package com.openlab.qualitos.quality.aiqms.infrastructure;

import com.openlab.qualitos.quality.aiqms.domain.AiQms;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class AiQmsMapper {
    private AiQmsMapper() {}

    static AiQmsJpaEntity toEntity(AiQms q, AiQmsJpaEntity target) {
        AiQmsJpaEntity e = target != null ? target : new AiQmsJpaEntity();
        if (q.getId() != null) e.setId(q.getId());
        e.setTenantId(q.getTenantId());
        e.setReference(q.getReference());
        e.setVersion(q.getVersion());
        e.setName(q.getName());
        e.setDescription(q.getDescription());
        e.setRegulatoryComplianceStrategy(q.getRegulatoryComplianceStrategy());
        e.setDesignControlDescription(q.getDesignControlDescription());
        e.setQualityControlDescription(q.getQualityControlDescription());
        e.setDataManagementDescription(q.getDataManagementDescription());
        e.setRiskManagementDescription(q.getRiskManagementDescription());
        e.setPmmDescription(q.getPmmDescription());
        e.setRegulatorCommunicationDescription(q.getRegulatorCommunicationDescription());
        e.setResourceManagementDescription(q.getResourceManagementDescription());
        e.setSupplierMonitoringDescription(q.getSupplierMonitoringDescription());
        e.setCoveredAiSystemIdsCsv(uuidSetToCsv(q.getCoveredAiSystemIds()));
        e.setStatus(q.getStatus());
        e.setSubmittedAt(q.getSubmittedAt());
        e.setSubmittedByUserId(q.getSubmittedByUserId());
        e.setApprovedAt(q.getApprovedAt());
        e.setApprovedByUserId(q.getApprovedByUserId());
        e.setApprovalNotes(q.getApprovalNotes());
        e.setEffectiveFrom(q.getEffectiveFrom());
        e.setEffectiveTo(q.getEffectiveTo());
        e.setSupersededByQmsId(q.getSupersededByQmsId());
        e.setArchivedReason(q.getArchivedReason());
        e.setCreatedByUserId(q.getCreatedByUserId());
        e.setCreatedAt(q.getCreatedAt());
        e.setUpdatedAt(q.getUpdatedAt());
        return e;
    }

    static AiQms toDomain(AiQmsJpaEntity e) {
        return new AiQms(
                e.getId(), e.getTenantId(), e.getReference(), e.getVersion(),
                e.getName(), e.getDescription(),
                e.getRegulatoryComplianceStrategy(), e.getDesignControlDescription(),
                e.getQualityControlDescription(), e.getDataManagementDescription(),
                e.getRiskManagementDescription(), e.getPmmDescription(),
                e.getRegulatorCommunicationDescription(),
                e.getResourceManagementDescription(), e.getSupplierMonitoringDescription(),
                csvToUuidSet(e.getCoveredAiSystemIdsCsv()),
                e.getStatus(),
                e.getSubmittedAt(), e.getSubmittedByUserId(),
                e.getApprovedAt(), e.getApprovedByUserId(), e.getApprovalNotes(),
                e.getEffectiveFrom(), e.getEffectiveTo(),
                e.getSupersededByQmsId(), e.getArchivedReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private static String uuidSetToCsv(Set<UUID> s) {
        if (s == null || s.isEmpty()) return null;
        return s.stream().map(UUID::toString).collect(Collectors.joining(","));
    }

    private static Set<UUID> csvToUuidSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
