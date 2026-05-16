package com.openlab.qualitos.quality.processoragreements.infrastructure;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class ProcessorAgreementMapper {
    private ProcessorAgreementMapper() {}

    static ProcessorAgreementJpaEntity toEntity(ProcessorAgreement a,
                                                ProcessorAgreementJpaEntity target) {
        ProcessorAgreementJpaEntity e = target != null ? target : new ProcessorAgreementJpaEntity();
        if (a.getId() != null) e.setId(a.getId());
        e.setTenantId(a.getTenantId());
        e.setReference(a.getReference());
        e.setProcessorName(a.getProcessorName());
        e.setProcessorLegalEntity(a.getProcessorLegalEntity());
        e.setProcessorContact(a.getProcessorContact());
        e.setProcessorDpoContact(a.getProcessorDpoContact());
        e.setProcessorCountry(a.getProcessorCountry());
        e.setServicesDescription(a.getServicesDescription());
        e.setSubProcessorCategoriesCsv(strSetToCsv(a.getSubProcessorCategories()));
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(a.getLinkedProcessingActivityIds()));
        e.setThirdCountryTransfersCsv(strSetToCsv(a.getThirdCountryTransfers()));
        e.setTransferSafeguards(a.getTransferSafeguards());
        e.setContractDocumentUrl(a.getContractDocumentUrl());
        e.setSignedAt(a.getSignedAt());
        e.setEffectiveFrom(a.getEffectiveFrom());
        e.setExpirationDate(a.getExpirationDate());
        e.setSecurityMeasures(a.getSecurityMeasures());
        e.setBreachNotificationCommitmentHours(a.getBreachNotificationCommitmentHours());
        e.setAuditRights(a.isAuditRights());
        e.setAuditRightsNotes(a.getAuditRightsNotes());
        e.setDataReturnOrDeletionTerms(a.getDataReturnOrDeletionTerms());
        e.setStatus(a.getStatus());
        e.setTerminatedAt(a.getTerminatedAt());
        e.setTerminationReason(a.getTerminationReason());
        e.setCreatedByUserId(a.getCreatedByUserId());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    static ProcessorAgreement toDomain(ProcessorAgreementJpaEntity e) {
        return new ProcessorAgreement(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getProcessorName(), e.getProcessorLegalEntity(),
                e.getProcessorContact(), e.getProcessorDpoContact(),
                e.getProcessorCountry(), e.getServicesDescription(),
                csvToStrSet(e.getSubProcessorCategoriesCsv()),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                csvToStrSet(e.getThirdCountryTransfersCsv()),
                e.getTransferSafeguards(), e.getContractDocumentUrl(),
                e.getSignedAt(), e.getEffectiveFrom(), e.getExpirationDate(),
                e.getSecurityMeasures(), e.getBreachNotificationCommitmentHours(),
                e.isAuditRights(), e.getAuditRightsNotes(),
                e.getDataReturnOrDeletionTerms(),
                e.getStatus(), e.getTerminatedAt(), e.getTerminationReason(),
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
