package com.openlab.qualitos.quality.standards.normdoc.dossier.application;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocStatus;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO applicatifs du module de génération documentaire multi-documents (§8.8). */
public final class DossierDto {

    private DossierDto() {}

    /** Contexte tenant non sensible (jamais de PII, jamais de tenant_id technique). */
    public record TenantProfile(
            String organizationName, String industry, String size,
            String language, List<String> knownProcesses) {}

    /**
     * Démarrage d'un dossier (le tenant + l'acteur viennent du JWT, jamais d'ici).
     * {@code documentKeys} vide = plan complet par défaut.
     */
    public record StartRequest(
            UUID standardId, TenantProfile tenantProfile, List<String> documentKeys) {}

    /** Finalisation : signature humaine globale du responsable (l'acteur = sujet JWT). */
    public record FinalizeRequest(String signature, String notes) {}

    /** Vue d'une pièce du dossier (avec suivi de génération + réutilisation suggérée). */
    public record DocumentView(
            String key, NormDocKind kind, String label, DossierDocStatus status,
            UUID normDocId, UUID reuseSuggestedNormDocId, String failureReason,
            int sectionCount) {

        public static DocumentView of(DossierDocument d) {
            return new DocumentView(
                    d.getKey(), d.getKind(), d.getLabel(), d.getStatus(),
                    d.getNormDocId(), d.getReuseSuggestedNormDocId(),
                    d.getFailureReason(), d.getSections().size());
        }
    }

    /** Vue complète d'un dossier documentaire. */
    public record View(
            UUID id, UUID tenantId, UUID standardId, String standardCode, String standardName,
            String organizationName, String language, DossierStatus status, String aiProvider,
            List<DocumentView> documents,
            int totalCount, int generatedCount, int failedCount, int progressPercent,
            String integritySha256, String integritySignature, String anchorTxRef,
            Instant finalizedAt, UUID finalizedByUserId,
            UUID createdByUserId, Instant createdAt, Instant updatedAt) {

        public static View of(DocumentationDossier d) {
            return new View(
                    d.getId(), d.getTenantId(), d.getStandardId(), d.getStandardCode(),
                    d.getStandardName(), d.getOrganizationName(), d.getLanguage(),
                    d.getStatus(), d.getAiProvider(),
                    d.getDocuments().stream().map(DocumentView::of).toList(),
                    d.totalCount(), d.generatedCount(), d.failedCount(), d.progressPercent(),
                    d.getIntegritySha256(), d.getIntegritySignature(), d.getAnchorTxRef(),
                    d.getFinalizedAt(), d.getFinalizedByUserId(),
                    d.getCreatedByUserId(), d.getCreatedAt(), d.getUpdatedAt());
        }
    }

    /** Référence d'une pièce réutilisable (réutilisation transversale §8.9). */
    public record ReuseSuggestion(
            String documentKey, NormDocKind kind, UUID normDocId,
            UUID standardId, String standardCode, String title) {}
}
