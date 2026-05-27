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

    // ---- Roadmap de certification (§8.5) ----

    public record RoadmapStageResponse(
            UUID id,
            int stepNumber,
            String name,
            String description,
            String typicalDuration,
            String deliverables,
            String responsibleRole,
            String involvedModules,
            StageStatus status,
            UUID assigneeId,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate actualStartDate,
            LocalDate actualEndDate,
            String notes,
            int orderIndex
    ) {}

    public record UpdateStageRequest(
            StageStatus status,
            UUID assigneeId,
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate actualStartDate,
            LocalDate actualEndDate,
            String notes
    ) {}

    /** Synthèse de progression de la roadmap (pour pilotage / dashboard). */
    public record RoadmapSummary(
            UUID tenantStandardId,
            int totalStages,
            int doneStages,
            int inProgressStages,
            int skippedStages,
            double completionPercent,
            List<RoadmapStageResponse> stages
    ) {}

    // ---- Audit blanc / gap analysis (§8.4 onglet 7, §8.7) ----

    /**
     * Rapport d'audit blanc : simulation d'audit avant l'audit réel.
     * Confronte les preuves liées aux exigences et produit un score de
     * préparation, la liste des écarts (findings) et un plan de remédiation.
     */
    public record AuditBlancReport(
            UUID tenantStandardId,
            UUID standardId,
            String standardCode,
            String standardName,
            Instant generatedAt,
            double readinessScore,
            int totalRequirements,
            int coveredRequirements,
            int mustTotal,
            int mustCovered,
            int criticalGaps,
            int majorGaps,
            int minorGaps,
            String verdict,
            List<AuditFinding> findings
    ) {}

    /**
     * Un écart constaté lors de l'audit blanc (exigence non couverte par une preuve),
     * avec une action de remédiation suggérée et sa priorité.
     */
    public record AuditFinding(
            UUID requirementId,
            String sectionCode,
            String clauseCode,
            String requirementCode,
            String requirementText,
            ObligationLevel obligation,
            RiskLevel riskIfMissing,
            String findingSeverity,
            String expectedEvidence,
            String remediationAction,
            int remediationPriority
    ) {}

    // ---- Dossier de certification (§8.4 onglet 6) ----

    /**
     * Dossier de certification complet, généré à la demande, intégrant
     * exigences, roadmap, alignement, audit blanc et preuves. Le contenu HTML
     * est imprimable en PDF ; son empreinte SHA-256 est ancrée (blockchain)
     * pour garantir l'intégrité à l'instant T (audit-ready, §8.7).
     */
    public record DossierResponse(
            UUID tenantStandardId,
            String standardCode,
            String standardName,
            Instant generatedAt,
            String sha256,
            String anchorTxRef,
            String fileName,
            String contentType,
            double readinessScore,
            double roadmapCompletion,
            int evidenceCount,
            String htmlContent,
            // Enveloppe de signature hybride (Ed25519 + ML-DSA-65) du SHA-256 du dossier,
            // encodée Base64URL (cf. ADR 0011). Vérifiable a posteriori.
            String signature
    ) {}

    // ---- Certification à blanc (§8.5 étapes 14-15 ; ISO/IEC 17021-1) ----

    /**
     * Simulation de l'audit de certification (« certification à blanc »), distincte
     * de l'audit blanc (gap analysis). Reproduit la logique d'un organisme
     * certificateur accrédité ISO/IEC 17021-1 : audit en deux étapes (revue
     * documentaire §4-7 puis audit terrain §8-10), classification des
     * non-conformités en <b>majeures</b> / <b>mineures</b> / <b>observations</b>,
     * puis décision de certification. Un certificat à blanc est émis lorsque la
     * décision est (conditionnellement) favorable — <i>sans valeur d'accréditation</i>.
     * Le verdict est haché, signé (hybride PQ, ADR 0011) et ancré (audit-ready, §8.7).
     */
    public record CertificationBlancReport(
            UUID tenantStandardId,
            UUID standardId,
            String standardCode,
            String standardName,
            Instant generatedAt,
            AuditStageResult stage1,        // revue documentaire (§4-7)
            AuditStageResult stage2,        // audit terrain (§8-10)
            int majorNonConformities,
            int minorNonConformities,
            int observations,
            String decision,               // CERTIFIABLE | CERTIFIABLE_SOUS_RESERVE | NON_CERTIFIABLE | AJOURNE
            String decisionLabel,
            List<NonConformity> nonConformities,
            MockCertificate certificate,   // null si non certifiable / ajourné
            String sha256,
            String anchorTxRef,
            String signature
    ) {}

    /** Résultat d'une étape de l'audit de certification (1 = documentaire, 2 = terrain). */
    public record AuditStageResult(
            int stageNumber,
            String name,
            boolean passed,
            double score,                  // % de couverture des exigences du périmètre de l'étape
            String summary,
            List<String> watchPoints       // points d'attention (codes d'exigences)
    ) {}

    /** Non-conformité relevée lors de la certification à blanc. */
    public record NonConformity(
            UUID requirementId,
            String sectionCode,
            String clauseCode,
            String requirementCode,
            String requirementText,
            String type,                   // MAJOR | MINOR | OBSERVATION
            String description,
            String requiredCorrection
    ) {}

    /**
     * Certificat à blanc émis quand le verdict est favorable ou favorable sous
     * réserve. Porte un avertissement explicite : il s'agit d'une simulation
     * interne, sans valeur d'accréditation (COFRAC/UKAS…).
     */
    public record MockCertificate(
            String certificateNumber,
            String standardCode,
            String scope,
            String certificationBody,
            Instant issuedAt,
            Instant expiresAt,
            String conditions,
            String disclaimer
    ) {}

    // ---- Bibliothèque documentaire (§8.4 onglet 3) ----

    public record DocumentTemplateResponse(
            UUID id,
            String code,
            String name,
            DocumentObligation obligation,
            String category,
            String format,            // dérivé de l'extension du modèle (MD, DOCX, BPMN…)
            String mapsToClauses,
            String description,
            boolean downloadable
    ) {}

    // ---- Cartographie des processus (§8.4 onglet 4) ----

    public record ProcessTemplateResponse(
            UUID id,
            String code,
            String name,
            String description,
            String mapsToClauses,
            String bpmnUri,
            int orderIndex
    ) {}

    // ---- Veille normative (§8.4 onglet 8) ----

    public record RevisionResponse(
            UUID id,
            String version,
            RevisionStatus status,
            LocalDate publishedDate,
            LocalDate effectiveDate,
            String summary,
            String impactNote,
            String sourceUrl,
            int orderIndex
    ) {}
}
