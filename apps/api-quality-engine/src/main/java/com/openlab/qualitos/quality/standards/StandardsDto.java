package com.openlab.qualitos.quality.standards;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class StandardsDto {

    private StandardsDto() {}

    // ---- Catalog (read-only) ----

    public record StandardSummary(
            UUID id,
            String code,
            String fullName,
            String publisher,
            String currentVersion,
            String family,
            String applicableIndustries,
            StandardStatus status,
            Integer recertificationCycleMonths
    ) {}

    public record StandardDetail(
            UUID id,
            String code,
            String fullName,
            String publisher,
            String currentVersion,
            LocalDate publicationDate,
            String family,
            String applicableIndustries,
            String description,
            boolean certificationBodyRequired,
            Integer recertificationCycleMonths,
            String relatedNormCodes,
            StandardStatus status,
            List<SectionDetail> sections
    ) {}

    public record SectionDetail(
            UUID id, String code, String title, String description,
            Integer orderIndex, List<ClauseDetail> clauses) {}

    public record ClauseDetail(
            UUID id, String code, String title, String description,
            Integer orderIndex, List<RequirementDetail> requirements) {}

    public record RequirementDetail(
            UUID id, String code, String text, ObligationLevel obligation,
            String evidenceTypes, String measurableCriteria, RiskLevel riskIfMissing,
            Integer orderIndex) {}

    // ---- Adoptions ----

    public record AdoptRequest(
            @NotNull UUID standardId,
            String scopeDescription,
            LocalDate targetCertificationDate,
            UUID leadAuditorId,
            @Size(max = 255) String certificationBody
    ) {}

    public record UpdateAdoptionRequest(
            String scopeDescription,
            LocalDate targetCertificationDate,
            UUID leadAuditorId,
            @Size(max = 255) String certificationBody
    ) {}

    public record CertifyRequest(
            @NotNull Instant certifiedAt,
            Instant expiresAt
    ) {}

    public record AdoptionResponse(
            UUID id,
            UUID tenantId,
            UUID standardId,
            String standardCode,
            String standardName,
            AdoptionStatus status,
            String scopeDescription,
            LocalDate targetCertificationDate,
            UUID leadAuditorId,
            String certificationBody,
            Instant certifiedAt,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    // ---- Evidence ----

    public record LinkEvidenceRequest(
            @NotNull UUID requirementId,
            @NotNull EvidenceType evidenceType,
            UUID evidenceRefId,
            @Size(max = 1024) String evidenceUri,
            String note,
            @NotNull UUID linkedBy
    ) {}

    public record EvidenceResponse(
            UUID id,
            UUID tenantStandardId,
            UUID requirementId,
            String requirementCode,
            EvidenceType evidenceType,
            UUID evidenceRefId,
            String evidenceUri,
            String note,
            UUID linkedBy,
            Instant linkedAt
    ) {}

    // ---- Alignment ----

    public record AlignmentReport(
            UUID tenantStandardId,
            UUID standardId,
            String standardCode,
            double overallScore,
            int totalRequirements,
            int coveredRequirements,
            int totalMustRequirements,
            int coveredMustRequirements,
            List<SectionAlignment> sections
    ) {}

    public record SectionAlignment(
            UUID sectionId,
            String sectionCode,
            String sectionTitle,
            double score,
            int totalRequirements,
            int coveredRequirements,
            List<ClauseAlignment> clauses
    ) {}

    public record ClauseAlignment(
            UUID clauseId,
            String clauseCode,
            String clauseTitle,
            double score,
            int totalRequirements,
            int coveredRequirements
    ) {}
}
