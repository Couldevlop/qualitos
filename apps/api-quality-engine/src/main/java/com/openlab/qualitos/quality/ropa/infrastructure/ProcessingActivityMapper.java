package com.openlab.qualitos.quality.ropa.infrastructure;

import com.openlab.qualitos.quality.ropa.domain.ProcessingActivity;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class ProcessingActivityMapper {
    private ProcessingActivityMapper() {}

    static ProcessingActivityJpaEntity toEntity(ProcessingActivity a, ProcessingActivityJpaEntity target) {
        ProcessingActivityJpaEntity e = target != null ? target : new ProcessingActivityJpaEntity();
        if (a.getId() != null) e.setId(a.getId());
        e.setTenantId(a.getTenantId());
        e.setReference(a.getReference());
        e.setName(a.getName());
        e.setPurposes(a.getPurposes());
        e.setLawfulBasis(a.getLawfulBasis());
        e.setLawfulBasisDetails(a.getLawfulBasisDetails());
        e.setControllerName(a.getControllerName());
        e.setControllerContact(a.getControllerContact());
        e.setDpoContact(a.getDpoContact());
        e.setJointControllerName(a.getJointControllerName());
        e.setJointControllerContact(a.getJointControllerContact());
        e.setDataSubjectCategoriesCsv(strSetToCsv(a.getDataSubjectCategories()));
        e.setDataCategoriesCsv(strSetToCsv(a.getDataCategories()));
        e.setSpecialCategoriesProcessed(a.isSpecialCategoriesProcessed());
        e.setSpecialCategoriesJustification(a.getSpecialCategoriesJustification());
        e.setRecipientCategoriesCsv(strSetToCsv(a.getRecipientCategories()));
        e.setThirdCountryTransfersCsv(strSetToCsv(a.getThirdCountryTransfers()));
        e.setTransferSafeguards(a.getTransferSafeguards());
        e.setLinkedRetentionRuleIdsCsv(uuidSetToCsv(a.getLinkedRetentionRuleIds()));
        e.setTechnicalMeasures(a.getTechnicalMeasures());
        e.setOrganizationalMeasures(a.getOrganizationalMeasures());
        e.setStatus(a.getStatus());
        e.setEffectiveFrom(a.getEffectiveFrom());
        e.setEffectiveTo(a.getEffectiveTo());
        e.setCreatedByUserId(a.getCreatedByUserId());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    static ProcessingActivity toDomain(ProcessingActivityJpaEntity e) {
        return new ProcessingActivity(
                e.getId(), e.getTenantId(), e.getReference(), e.getName(),
                e.getPurposes(),
                e.getLawfulBasis(), e.getLawfulBasisDetails(),
                e.getControllerName(), e.getControllerContact(), e.getDpoContact(),
                e.getJointControllerName(), e.getJointControllerContact(),
                csvToStrSet(e.getDataSubjectCategoriesCsv()),
                csvToStrSet(e.getDataCategoriesCsv()),
                e.isSpecialCategoriesProcessed(), e.getSpecialCategoriesJustification(),
                csvToStrSet(e.getRecipientCategoriesCsv()),
                csvToStrSet(e.getThirdCountryTransfersCsv()),
                e.getTransferSafeguards(),
                csvToUuidSet(e.getLinkedRetentionRuleIdsCsv()),
                e.getTechnicalMeasures(), e.getOrganizationalMeasures(),
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
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
