package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agrégat — dossier documentaire généré en lot par l'IA pour l'adoption d'une
 * norme (Standards Hub §8.8, génération avancée multi-documents). Orchestre la
 * génération d'un ensemble de pièces (Manuel Qualité, Politique Qualité,
 * procédures documentées) puis le suivi de leur cycle de validation humaine.
 *
 * <p>Domaine PUR : aucune dépendance Spring/JPA.
 *
 * <p>Garde-fous (invariant §18.2 #5 — « l'IA suggère, l'humain décide ») :
 * <ul>
 *   <li>le dossier n'est {@link DossierStatus#FINALISE} (signé + ancré) que si
 *       TOUTES ses pièces générées sont effectivement approuvées par un humain ;</li>
 *   <li>la signature/l'ancrage sont posés une seule fois (idempotence d'état) ;</li>
 *   <li>le tenant est porté par l'agrégat (jamais lu d'un body).</li>
 * </ul>
 */
public final class DocumentationDossier {

    private UUID id;
    private final UUID tenantId;
    private final UUID standardId;
    private final String standardCode;
    private final String standardName;
    private final String organizationName;
    private final String language;
    private final List<DossierDocument> documents;

    private DossierStatus status;
    private String aiProvider;

    private String integritySha256;
    private String integritySignature;
    private String anchorTxRef;
    private Instant finalizedAt;
    private UUID finalizedByUserId;

    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public DocumentationDossier(UUID id, UUID tenantId, UUID standardId, String standardCode,
                                String standardName, String organizationName, String language,
                                List<DossierDocument> documents, DossierStatus status,
                                String aiProvider, String integritySha256,
                                String integritySignature, String anchorTxRef,
                                Instant finalizedAt, UUID finalizedByUserId,
                                UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.standardId = Objects.requireNonNull(standardId, "standardId");
        this.standardCode = requireText(standardCode, "standardCode", 100);
        this.standardName = requireText(standardName, "standardName", 500);
        this.organizationName = requireText(organizationName, "organizationName", 500);
        this.language = (language == null || language.isBlank()) ? "fr" : language;
        this.documents = sanitizeDocuments(documents);
        this.status = Objects.requireNonNull(status, "status");
        this.aiProvider = aiProvider;
        this.integritySha256 = integritySha256;
        this.integritySignature = integritySignature;
        this.anchorTxRef = anchorTxRef;
        this.finalizedAt = finalizedAt;
        this.finalizedByUserId = finalizedByUserId;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    /** Démarre un dossier : pièces planifiées (toutes EN_ATTENTE). */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static DocumentationDossier start(UUID tenantId, UUID standardId, String standardCode,
                                             String standardName, String organizationName,
                                             String language, List<DossierDocument> documents,
                                             UUID createdByUserId, Instant now) {
        return new DocumentationDossier(null, tenantId, standardId, standardCode, standardName,
                organizationName, language, documents, DossierStatus.GENERATION_EN_COURS,
                null, null, null, null, null, null, createdByUserId, now, now);
    }

    /** Mémorise le provider IA effectif (après le premier appel réussi). */
    public void recordProvider(String provider, Instant now) {
        if (provider != null && !provider.isBlank()) {
            this.aiProvider = provider;
        }
        this.updatedAt = now;
    }

    /**
     * Recalcule le statut global du dossier d'après l'état de ses pièces :
     * tant qu'une pièce est en attente / en cours, on reste en génération ;
     * sinon (toutes générées, réutilisées ou en échec) on passe à GENERE.
     * Ne dégrade jamais un dossier déjà FINALISE.
     */
    public void refreshGenerationStatus(Instant now) {
        if (status == DossierStatus.FINALISE) {
            return;
        }
        boolean anyPending = documents.stream().anyMatch(DossierDocument::isPending);
        this.status = anyPending ? DossierStatus.GENERATION_EN_COURS : DossierStatus.GENERE;
        this.updatedAt = now;
    }

    /**
     * Finalise le dossier : pose l'empreinte SHA-256 signée + la référence
     * d'ancrage blockchain. Exige que TOUTES les pièces générées soient
     * effectivement approuvées (validation humaine, §18.2 #5) et qu'au moins une
     * pièce existe. L'opération est interdite si une pièce est encore en attente.
     */
    public void finalize(String sha256, String signature, String anchorRef,
                         UUID finalizerId, Instant now) {
        if (status == DossierStatus.FINALISE) {
            throw new DossierStateException("Le dossier est déjà finalisé");
        }
        if (documents.stream().anyMatch(DossierDocument::isPending)) {
            throw new DossierStateException(
                    "Génération non terminée : impossible de finaliser le dossier");
        }
        if (generatedDocumentIds().isEmpty()) {
            throw new DossierStateException("Aucun document généré à finaliser");
        }
        requireText(sha256, "sha256", 64);
        requireText(signature, "signature", 100_000);
        requireText(anchorRef, "anchorRef", 300);
        this.integritySha256 = sha256;
        this.integritySignature = signature;
        this.anchorTxRef = anchorRef;
        this.finalizedByUserId = Objects.requireNonNull(finalizerId, "finalizerId");
        this.finalizedAt = now;
        this.status = DossierStatus.FINALISE;
        this.updatedAt = now;
    }

    /** Pièce identifiée par sa clé (immutable lookup). */
    public DossierDocument document(String key) {
        return documents.stream().filter(d -> d.getKey().equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown document key: " + key));
    }

    /** Identifiants des {@code NormativeDocument} générés (pièces liées). */
    public List<UUID> generatedDocumentIds() {
        return documents.stream()
                .map(DossierDocument::getNormDocId)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Progression : nombre de pièces générées sur le total. */
    public int generatedCount() {
        return (int) documents.stream().filter(DossierDocument::isGenerated).count();
    }

    public int failedCount() {
        return (int) documents.stream().filter(DossierDocument::isFailed).count();
    }

    public int totalCount() {
        return documents.size();
    }

    /** Pourcentage de progression (0–100, arrondi). */
    public int progressPercent() {
        if (documents.isEmpty()) {
            return 0;
        }
        long done = documents.stream().filter(d -> !d.isPending()).count();
        return (int) Math.round(done * 100.0 / documents.size());
    }

    public boolean isFinalized() {
        return status == DossierStatus.FINALISE;
    }

    public void assignId(UUID id) {
        this.id = id;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStandardId() { return standardId; }
    public String getStandardCode() { return standardCode; }
    public String getStandardName() { return standardName; }
    public String getOrganizationName() { return organizationName; }
    public String getLanguage() { return language; }
    public List<DossierDocument> getDocuments() { return documents; }
    public DossierStatus getStatus() { return status; }
    public String getAiProvider() { return aiProvider; }
    public String getIntegritySha256() { return integritySha256; }
    public String getIntegritySignature() { return integritySignature; }
    public String getAnchorTxRef() { return anchorTxRef; }
    public Instant getFinalizedAt() { return finalizedAt; }
    public UUID getFinalizedByUserId() { return finalizedByUserId; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    private static List<DossierDocument> sanitizeDocuments(List<DossierDocument> input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("at least one document required in a dossier");
        }
        List<DossierDocument> out = new ArrayList<>();
        Set<String> keys = new LinkedHashSet<>();
        for (DossierDocument d : input) {
            if (d == null) {
                throw new IllegalArgumentException("null document not allowed");
            }
            if (!keys.add(d.getKey())) {
                throw new IllegalArgumentException("document keys must be unique: " + d.getKey());
            }
            out.add(d);
        }
        return Collections.unmodifiableList(out);
    }

    private static String requireText(String v, String f, int maxLen) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(f + " required");
        }
        if (v.length() > maxLen) {
            throw new IllegalArgumentException(f + " too long (max " + maxLen + ")");
        }
        return v;
    }
}
