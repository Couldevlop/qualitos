package com.openlab.qualitos.quality.automateddecisions.infrastructure;

import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class AutomatedDecisionMapper {
    private AutomatedDecisionMapper() {}

    static AutomatedDecisionJpaEntity toEntity(AutomatedDecisionRecord r,
                                               AutomatedDecisionJpaEntity target) {
        AutomatedDecisionJpaEntity e = target != null ? target : new AutomatedDecisionJpaEntity();
        if (r.getId() != null) e.setId(r.getId());
        e.setTenantId(r.getTenantId());
        e.setReference(r.getReference());
        e.setName(r.getName());
        e.setDescription(r.getDescription());
        e.setDecisionType(r.getDecisionType());
        e.setArt22LawfulBasis(r.getArt22LawfulBasis());
        e.setLawfulBasisDetails(r.getLawfulBasisDetails());
        e.setInputDataCategoriesCsv(strSetToCsv(r.getInputDataCategories()));
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(r.getLinkedProcessingActivityIds()));
        e.setLinkedDpiaId(r.getLinkedDpiaId());
        e.setAlgorithmDescription(r.getAlgorithmDescription());
        e.setSignificanceForSubject(r.getSignificanceForSubject());
        e.setHumanReviewMechanism(r.getHumanReviewMechanism());
        e.setObjectionMechanism(r.getObjectionMechanism());
        e.setStatus(r.getStatus());
        e.setEffectiveFrom(r.getEffectiveFrom());
        e.setEffectiveTo(r.getEffectiveTo());
        e.setCreatedByUserId(r.getCreatedByUserId());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        return e;
    }

    static AutomatedDecisionRecord toDomain(AutomatedDecisionJpaEntity e) {
        return new AutomatedDecisionRecord(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getName(), e.getDescription(),
                e.getDecisionType(),
                e.getArt22LawfulBasis(), e.getLawfulBasisDetails(),
                csvToStrSet(e.getInputDataCategoriesCsv()),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                e.getLinkedDpiaId(),
                e.getAlgorithmDescription(), e.getSignificanceForSubject(),
                e.getHumanReviewMechanism(), e.getObjectionMechanism(),
                e.getStatus(), e.getEffectiveFrom(), e.getEffectiveTo(),
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
        return Arrays.stream(csv.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<UUID> csvToUuidSet(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return Arrays.stream(csv.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
