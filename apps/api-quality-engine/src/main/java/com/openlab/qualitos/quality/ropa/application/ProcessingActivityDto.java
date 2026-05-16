package com.openlab.qualitos.quality.ropa.application;

import com.openlab.qualitos.quality.ropa.domain.LawfulBasis;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivity;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class ProcessingActivityDto {

    private ProcessingActivityDto() {}

    public record CreateRequest(
            String reference,
            String name,
            String purposes,
            LawfulBasis lawfulBasis,
            String lawfulBasisDetails,
            String controllerName,
            String controllerContact,
            String dpoContact,
            String jointControllerName,
            String jointControllerContact,
            Set<String> dataSubjectCategories,
            Set<String> dataCategories,
            boolean specialCategoriesProcessed,
            String specialCategoriesJustification,
            Set<String> recipientCategories,
            Set<String> thirdCountryTransfers,
            String transferSafeguards,
            Set<UUID> linkedRetentionRuleIds,
            String technicalMeasures,
            String organizationalMeasures,
            UUID createdByUserId) {}

    public record EditRequest(
            String name,
            String purposes,
            LawfulBasis lawfulBasis,
            String lawfulBasisDetails,
            String controllerName,
            String controllerContact,
            String dpoContact,
            String jointControllerName,
            String jointControllerContact,
            Set<String> dataSubjectCategories,
            Set<String> dataCategories,
            boolean specialCategoriesProcessed,
            String specialCategoriesJustification,
            Set<String> recipientCategories,
            Set<String> thirdCountryTransfers,
            String transferSafeguards,
            Set<UUID> linkedRetentionRuleIds,
            String technicalMeasures,
            String organizationalMeasures) {}

    public record View(
            UUID id, UUID tenantId, String reference, String name,
            String purposes,
            LawfulBasis lawfulBasis, String lawfulBasisDetails,
            String controllerName, String controllerContact, String dpoContact,
            String jointControllerName, String jointControllerContact,
            Set<String> dataSubjectCategories, Set<String> dataCategories,
            boolean specialCategoriesProcessed, String specialCategoriesJustification,
            Set<String> recipientCategories, Set<String> thirdCountryTransfers,
            String transferSafeguards, Set<UUID> linkedRetentionRuleIds,
            String technicalMeasures, String organizationalMeasures,
            ProcessingActivityStatus status,
            Instant effectiveFrom, Instant effectiveTo,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(ProcessingActivity a) {
            return new View(
                    a.getId(), a.getTenantId(), a.getReference(), a.getName(),
                    a.getPurposes(),
                    a.getLawfulBasis(), a.getLawfulBasisDetails(),
                    a.getControllerName(), a.getControllerContact(), a.getDpoContact(),
                    a.getJointControllerName(), a.getJointControllerContact(),
                    a.getDataSubjectCategories(), a.getDataCategories(),
                    a.isSpecialCategoriesProcessed(), a.getSpecialCategoriesJustification(),
                    a.getRecipientCategories(), a.getThirdCountryTransfers(),
                    a.getTransferSafeguards(), a.getLinkedRetentionRuleIds(),
                    a.getTechnicalMeasures(), a.getOrganizationalMeasures(),
                    a.getStatus(), a.getEffectiveFrom(), a.getEffectiveTo(),
                    a.getCreatedByUserId(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }
}
