package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class CrossBorderTransferMapper {
    private CrossBorderTransferMapper() {}

    static CrossBorderTransferJpaEntity toEntity(CrossBorderTransfer t,
                                                 CrossBorderTransferJpaEntity target) {
        CrossBorderTransferJpaEntity e = target != null ? target : new CrossBorderTransferJpaEntity();
        if (t.getId() != null) e.setId(t.getId());
        e.setTenantId(t.getTenantId());
        e.setReference(t.getReference());
        e.setRecipientName(t.getRecipientName());
        e.setRecipientLegalEntity(t.getRecipientLegalEntity());
        e.setRecipientContact(t.getRecipientContact());
        e.setDestinationCountriesCsv(strSetToCsv(t.getDestinationCountries()));
        e.setMechanism(t.getMechanism());
        e.setSafeguardsDescription(t.getSafeguardsDescription());
        e.setSafeguardsDocumentUrl(t.getSafeguardsDocumentUrl());
        e.setDerogationJustification(t.getDerogationJustification());
        e.setDataCategoriesCsv(strSetToCsv(t.getDataCategories()));
        e.setLinkedProcessingActivityIdsCsv(uuidSetToCsv(t.getLinkedProcessingActivityIds()));
        e.setLinkedProcessorAgreementIdsCsv(uuidSetToCsv(t.getLinkedProcessorAgreementIds()));
        e.setStatus(t.getStatus());
        e.setEffectiveFrom(t.getEffectiveFrom());
        e.setEffectiveTo(t.getEffectiveTo());
        e.setSuspendedAt(t.getSuspendedAt());
        e.setSuspensionReason(t.getSuspensionReason());
        e.setTerminationReason(t.getTerminationReason());
        e.setCreatedByUserId(t.getCreatedByUserId());
        e.setCreatedAt(t.getCreatedAt());
        e.setUpdatedAt(t.getUpdatedAt());
        return e;
    }

    static CrossBorderTransfer toDomain(CrossBorderTransferJpaEntity e) {
        return new CrossBorderTransfer(
                e.getId(), e.getTenantId(), e.getReference(),
                e.getRecipientName(), e.getRecipientLegalEntity(), e.getRecipientContact(),
                csvToStrSet(e.getDestinationCountriesCsv()),
                e.getMechanism(),
                e.getSafeguardsDescription(), e.getSafeguardsDocumentUrl(),
                e.getDerogationJustification(),
                csvToStrSet(e.getDataCategoriesCsv()),
                csvToUuidSet(e.getLinkedProcessingActivityIdsCsv()),
                csvToUuidSet(e.getLinkedProcessorAgreementIdsCsv()),
                e.getStatus(),
                e.getEffectiveFrom(), e.getEffectiveTo(),
                e.getSuspendedAt(), e.getSuspensionReason(), e.getTerminationReason(),
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
