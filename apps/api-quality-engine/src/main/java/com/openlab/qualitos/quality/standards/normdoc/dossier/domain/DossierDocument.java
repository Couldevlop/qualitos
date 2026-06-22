package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pièce d'un dossier documentaire (value object mutable du domaine pur). Décrit
 * UN document cible à générer en lot : sa clé stable, son type, son libellé, la
 * structure des sections à rédiger, son statut de génération et le lien vers le
 * {@code NormativeDocument} produit (cycle de validation humaine séparé).
 *
 * <p>Domaine PUR : aucune dépendance Spring/JPA. Les transitions de statut de
 * génération sont contrôlées par l'agrégat parent {@link DocumentationDossier}.
 */
public final class DossierDocument {

    private final String key;
    private final NormDocKind kind;
    private final String label;
    private final List<SectionPlan> sections;

    private DossierDocStatus status;
    private UUID normDocId;
    private UUID reuseSuggestedNormDocId;
    private String failureReason;

    /** Plan d'une section : clé, titre, clauses couvertes, consigne de rédaction. */
    public record SectionPlan(String key, String title, List<String> clauses, String guidance) {
        public SectionPlan {
            key = requireText(key, "section key");
            title = requireText(title, "section title");
            clauses = clauses == null ? List.of() : List.copyOf(clauses);
            guidance = guidance == null ? "" : guidance;
        }

        private static String requireText(String v, String field) {
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException(field + " required");
            }
            return v;
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DossierDocument(String key, NormDocKind kind, String label, List<SectionPlan> sections,
                           DossierDocStatus status, UUID normDocId,
                           UUID reuseSuggestedNormDocId, String failureReason) {
        this.key = requireText(key, "document key", 100);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.label = requireText(label, "label", 500);
        this.sections = sanitizeSections(sections);
        this.status = Objects.requireNonNull(status, "status");
        this.normDocId = normDocId;
        this.reuseSuggestedNormDocId = reuseSuggestedNormDocId;
        this.failureReason = failureReason;
    }

    /** Pièce fraîchement planifiée (en attente de génération). */
    public static DossierDocument planned(String key, NormDocKind kind, String label,
                                          List<SectionPlan> sections) {
        return new DossierDocument(key, kind, label, sections,
                DossierDocStatus.EN_ATTENTE, null, null, null);
    }

    /** Marque le début de la génération IA de cette pièce. */
    public void markGenerating() {
        this.status = DossierDocStatus.EN_GENERATION;
        this.failureReason = null;
    }

    /** Pièce générée : lie le brouillon IA produit. */
    public void markGenerated(UUID producedNormDocId) {
        this.normDocId = Objects.requireNonNull(producedNormDocId, "normDocId");
        this.status = DossierDocStatus.GENERE;
        this.failureReason = null;
    }

    /** Génération échouée (timeout / erreur IA) — la pièce reste relançable. */
    public void markFailed(String reason) {
        this.status = DossierDocStatus.ECHEC;
        this.failureReason = (reason == null || reason.isBlank())
                ? "génération IA indisponible" : trim(reason, 2000);
        this.normDocId = null;
    }

    /** Marque la pièce comme réutilisable depuis un document déjà approuvé (§8.9). */
    public void suggestReuse(UUID equivalentNormDocId) {
        this.reuseSuggestedNormDocId = equivalentNormDocId;
    }

    public boolean isGenerated() {
        return status == DossierDocStatus.GENERE;
    }

    public boolean isPending() {
        return status == DossierDocStatus.EN_ATTENTE || status == DossierDocStatus.EN_GENERATION;
    }

    public boolean isFailed() {
        return status == DossierDocStatus.ECHEC;
    }

    public String getKey() { return key; }
    public NormDocKind getKind() { return kind; }
    public String getLabel() { return label; }
    public List<SectionPlan> getSections() { return sections; }
    public DossierDocStatus getStatus() { return status; }
    public UUID getNormDocId() { return normDocId; }
    public UUID getReuseSuggestedNormDocId() { return reuseSuggestedNormDocId; }
    public String getFailureReason() { return failureReason; }

    private static List<SectionPlan> sanitizeSections(List<SectionPlan> input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("at least one section required per document");
        }
        return List.copyOf(input);
    }

    private static String requireText(String v, String field, int maxLen) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(field + " required");
        }
        if (v.length() > maxLen) {
            throw new IllegalArgumentException(field + " too long (max " + maxLen + ")");
        }
        return v;
    }

    private static String trim(String v, int maxLen) {
        return v.length() > maxLen ? v.substring(0, maxLen) : v;
    }
}
