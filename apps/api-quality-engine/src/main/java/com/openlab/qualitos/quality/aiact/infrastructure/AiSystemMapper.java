package com.openlab.qualitos.quality.aiact.infrastructure;

import com.openlab.qualitos.quality.aiact.domain.AiSystem;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class AiSystemMapper {
    private AiSystemMapper() {}

    static AiSystemJpaEntity toEntity(AiSystem s, AiSystemJpaEntity target) {
        AiSystemJpaEntity e = target != null ? target : new AiSystemJpaEntity();
        if (s.getId() != null) e.setId(s.getId());
        e.setTenantId(s.getTenantId());
        e.setReference(s.getReference());
        e.setName(s.getName());
        e.setDescription(s.getDescription());
        e.setProviderName(s.getProviderName());
        e.setIntendedPurpose(s.getIntendedPurpose());
        e.setRiskClassification(s.getRiskClassification());
        e.setRole(s.getRole());
        e.setGeneralPurpose(s.isGeneralPurpose());
        e.setStatus(s.getStatus());
        e.setConformityAssessmentEvidenceUrl(s.getConformityAssessmentEvidenceUrl());
        e.setCeMarkingNumber(s.getCeMarkingNumber());
        e.setHumanOversightDescription(s.getHumanOversightDescription());
        e.setTransparencyMeasures(s.getTransparencyMeasures());
        e.setDataGovernanceNotes(s.getDataGovernanceNotes());
        e.setLinkedDpiaId(s.getLinkedDpiaId());
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(s.getLinkedProcessingActivityIds()));
        e.setLinkedAutomatedDecisionIdsCsv(uuidSetToCsv(s.getLinkedAutomatedDecisionIds()));
        e.setEffectiveFrom(s.getEffectiveFrom());
        e.setEffectiveTo(s.getEffectiveTo());
        e.setWithdrawalReason(s.getWithdrawalReason());
        e.setCreatedByUserId(s.getCreatedByUserId());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }

    static AiSystem toDomain(AiSystemJpaEntity e) {
        return new AiSystem(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getName(), e.getDescription(),
                e.getProviderName(), e.getIntendedPurpose(),
                e.getRiskClassification(), e.getRole(), e.isGeneralPurpose(),
                e.getStatus(),
                e.getConformityAssessmentEvidenceUrl(), e.getCeMarkingNumber(),
                e.getHumanOversightDescription(), e.getTransparencyMeasures(),
                e.getDataGovernanceNotes(),
                e.getLinkedDpiaId(),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                csvToUuidSet(e.getLinkedAutomatedDecisionIdsCsv()),
                e.getEffectiveFrom(), e.getEffectiveTo(), e.getWithdrawalReason(),
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
