package com.openlab.qualitos.quality.standards.normdoc.application;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocSection;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO applicatifs du module de génération de documents normatifs (§8.8). */
public final class NormDocDto {

    private NormDocDto() {}

    /** Contexte tenant non sensible (jamais de PII, jamais de tenant_id technique). */
    public record TenantProfile(
            String organizationName, String industry, String size,
            String language, List<String> knownProcesses) {}

    /** Spécification d'une section à rédiger. */
    public record SectionSpec(String key, String title, List<String> clauses, String guidance) {}

    /** Commande de génération (le tenant + l'acteur viennent du JWT, jamais d'ici). */
    public record GenerateRequest(
            UUID standardId, NormDocKind kind, TenantProfile tenantProfile,
            List<SectionSpec> sections) {}

    /** Édition d'un brouillon. */
    public record EditRequest(String title, List<SectionView> sections) {}

    /** Approbation : signature humaine + notes (l'approbateur = sujet JWT). */
    public record ApproveRequest(String signature, String notes) {}

    /** Rejet : motif obligatoire. */
    public record RejectRequest(String reason) {}

    /** Vue d'une section. */
    public record SectionView(String key, String title, List<String> clauses, String bodyMarkdown) {
        public static SectionView of(NormDocSection s) {
            return new SectionView(s.getKey(), s.getTitle(), s.getClauses(), s.getBodyMarkdown());
        }

        public NormDocSection toDomain() {
            return new NormDocSection(key, title, clauses, bodyMarkdown);
        }
    }

    /** Vue complète d'un document normatif. */
    public record View(
            UUID id, UUID tenantId, UUID standardId, String standardCode,
            NormDocKind kind, String title, List<SectionView> sections,
            NormDocStatus status, String aiProvider, String markdown,
            Instant submittedAt, UUID submittedByUserId,
            Instant approvedAt, UUID approvedByUserId, String approvalNotes,
            String humanSignature, String rejectionReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt) {

        public static View of(NormativeDocument d) {
            return new View(
                    d.getId(), d.getTenantId(), d.getStandardId(), d.getStandardCode(),
                    d.getKind(), d.getTitle(),
                    d.getSections().stream().map(SectionView::of).toList(),
                    d.getStatus(), d.getAiProvider(), d.toMarkdown(),
                    d.getSubmittedAt(), d.getSubmittedByUserId(),
                    d.getApprovedAt(), d.getApprovedByUserId(), d.getApprovalNotes(),
                    d.getHumanSignature(), d.getRejectionReason(),
                    d.getCreatedByUserId(), d.getCreatedAt(), d.getUpdatedAt());
        }
    }
}
