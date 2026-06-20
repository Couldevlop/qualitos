package com.openlab.qualitos.quality.standards.normdoc.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agrégat — document normatif généré par l'IA puis validé par un humain
 * (Standards Hub §8.8, onglet 3). Domaine PUR : aucune dépendance Spring/JPA.
 *
 * <p>Le document naît en {@link NormDocStatus#BROUILLON_IA} (rédigé section par
 * section par la passerelle IA), peut être édité, puis soumis à revue
 * ({@link NormDocStatus#EN_VALIDATION}). Un humain l'approuve
 * ({@link NormDocStatus#APPROUVE}) — l'approbateur est l'acteur authentifié
 * (sujet du JWT), JAMAIS un identifiant fourni dans le body (OWASP A01,
 * CLAUDE.md §18.2 #2/#5). Aucune publication sans signature humaine.
 *
 * <p>Garde-fous (dupliqués en base) :
 * <ul>
 *   <li>BROUILLON_IA → APPROUVE interdit (revue obligatoire).</li>
 *   <li>seule la transition EN_VALIDATION → APPROUVE pose un approbateur + une signature.</li>
 *   <li>l'édition n'est possible qu'en BROUILLON_IA (ou après REJETE, revenu en brouillon).</li>
 * </ul>
 */
public final class NormativeDocument {

    private static final Map<NormDocStatus, Set<NormDocStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(NormDocStatus.class);
        ALLOWED.put(NormDocStatus.BROUILLON_IA, EnumSet.of(NormDocStatus.EN_VALIDATION));
        ALLOWED.put(NormDocStatus.EN_VALIDATION,
                EnumSet.of(NormDocStatus.APPROUVE, NormDocStatus.REJETE));
        ALLOWED.put(NormDocStatus.REJETE, EnumSet.of(NormDocStatus.BROUILLON_IA));
        ALLOWED.put(NormDocStatus.APPROUVE, EnumSet.noneOf(NormDocStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final UUID standardId;
    private final String standardCode;
    private final NormDocKind kind;
    private String title;
    private List<NormDocSection> sections;
    private NormDocStatus status;
    private final String aiProvider;

    private Instant submittedAt;
    private UUID submittedByUserId;
    private Instant approvedAt;
    private UUID approvedByUserId;
    private String approvalNotes;
    /** Signature humaine (empreinte) apposée à l'approbation. */
    private String humanSignature;
    private String rejectionReason;

    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("checkstyle:ParameterNumber")
    public NormativeDocument(UUID id, UUID tenantId, UUID standardId, String standardCode,
                             NormDocKind kind, String title, List<NormDocSection> sections,
                             NormDocStatus status, String aiProvider,
                             Instant submittedAt, UUID submittedByUserId,
                             Instant approvedAt, UUID approvedByUserId, String approvalNotes,
                             String humanSignature, String rejectionReason,
                             UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.standardId = Objects.requireNonNull(standardId, "standardId");
        this.standardCode = requireText(standardCode, "standardCode", 100);
        this.kind = Objects.requireNonNull(kind, "kind");
        this.title = requireText(title, "title", 500);
        this.sections = sanitizeSections(sections);
        this.status = Objects.requireNonNull(status, "status");
        this.aiProvider = aiProvider;
        this.submittedAt = submittedAt;
        this.submittedByUserId = submittedByUserId;
        this.approvedAt = approvedAt;
        this.approvedByUserId = approvedByUserId;
        this.approvalNotes = approvalNotes;
        this.humanSignature = humanSignature;
        this.rejectionReason = rejectionReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    /** Fabrique un brouillon IA fraîchement généré (section par section). */
    public static NormativeDocument draftFromAi(UUID tenantId, UUID standardId, String standardCode,
                                                NormDocKind kind, String title,
                                                List<NormDocSection> sections, String aiProvider,
                                                UUID createdByUserId, Instant now) {
        return new NormativeDocument(null, tenantId, standardId, standardCode, kind, title,
                sections, NormDocStatus.BROUILLON_IA, aiProvider,
                null, null, null, null, null, null, null,
                createdByUserId, now, now);
    }

    /** Édite le titre / les sections d'un brouillon (uniquement en BROUILLON_IA). */
    public void editDraft(String title, List<NormDocSection> sections, Instant now) {
        if (status != NormDocStatus.BROUILLON_IA) {
            throw new NormDocStateException("Only BROUILLON_IA documents can be edited");
        }
        this.title = requireText(title, "title", 500);
        this.sections = sanitizeSections(sections);
        this.updatedAt = now;
    }

    /** Soumet le document à la revue humaine. */
    public void submitForReview(UUID submitterId, Instant now) {
        ensureTransition(NormDocStatus.EN_VALIDATION);
        Objects.requireNonNull(submitterId, "submitterId");
        this.submittedByUserId = submitterId;
        this.submittedAt = now;
        // Une nouvelle soumission efface un éventuel rejet précédent.
        this.rejectionReason = null;
        this.status = NormDocStatus.EN_VALIDATION;
        this.updatedAt = now;
    }

    /**
     * Approuve et signe le document. L'approbateur (sujet JWT) doit différer du
     * soumetteur (séparation des tâches). La signature humaine est obligatoire.
     */
    public void approve(UUID approverId, String signature, String notes, Instant now) {
        ensureTransition(NormDocStatus.APPROUVE);
        Objects.requireNonNull(approverId, "approverId");
        if (signature == null || signature.isBlank()) {
            throw new NormDocStateException("Human signature required to approve");
        }
        if (approverId.equals(submittedByUserId)) {
            throw new NormDocStateException(
                    "Approver must differ from submitter (segregation of duties)");
        }
        this.approvedByUserId = approverId;
        this.approvedAt = now;
        this.humanSignature = signature;
        this.approvalNotes = notes;
        this.status = NormDocStatus.APPROUVE;
        this.updatedAt = now;
    }

    /** Rejette en revue : un motif est requis, le document revient en brouillon. */
    public void reject(String reason, Instant now) {
        ensureTransition(NormDocStatus.REJETE);
        if (reason == null || reason.isBlank()) {
            throw new NormDocStateException("Rejection reason required");
        }
        this.rejectionReason = reason;
        this.status = NormDocStatus.REJETE;
        this.updatedAt = now;
        // Repasse immédiatement en brouillon pour permettre la reprise.
        ensureTransition(NormDocStatus.BROUILLON_IA);
        this.status = NormDocStatus.BROUILLON_IA;
        this.submittedByUserId = null;
        this.submittedAt = null;
        this.updatedAt = now;
    }

    public boolean isDraft()      { return status == NormDocStatus.BROUILLON_IA; }
    public boolean isInReview()   { return status == NormDocStatus.EN_VALIDATION; }
    public boolean isApproved()   { return status == NormDocStatus.APPROUVE; }

    /** Sérialise le document complet en Markdown (titre H1 + sections H2). */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder("# ").append(title).append("\n\n");
        for (NormDocSection s : sections) {
            sb.append("## ").append(s.getTitle()).append("\n");
            if (!s.getClauses().isEmpty()) {
                sb.append("*Clauses : ").append(String.join(", ", s.getClauses())).append("*\n");
            }
            sb.append("\n").append(s.getBodyMarkdown().strip()).append("\n\n");
        }
        return sb.toString().strip() + "\n";
    }

    private void ensureTransition(NormDocStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new NormDocStateException(
                    "Transition " + status + " → " + target + " is not allowed");
        }
    }

    private static List<NormDocSection> sanitizeSections(List<NormDocSection> input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("at least one section required");
        }
        List<NormDocSection> out = new ArrayList<>();
        Set<String> keys = new java.util.LinkedHashSet<>();
        for (NormDocSection s : input) {
            if (s == null) {
                throw new IllegalArgumentException("null section not allowed");
            }
            if (!keys.add(s.getKey())) {
                throw new IllegalArgumentException("section keys must be unique: " + s.getKey());
            }
            out.add(s);
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

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStandardId() { return standardId; }
    public String getStandardCode() { return standardCode; }
    public NormDocKind getKind() { return kind; }
    public String getTitle() { return title; }
    public List<NormDocSection> getSections() { return sections; }
    public NormDocStatus getStatus() { return status; }
    public String getAiProvider() { return aiProvider; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getSubmittedByUserId() { return submittedByUserId; }
    public Instant getApprovedAt() { return approvedAt; }
    public UUID getApprovedByUserId() { return approvedByUserId; }
    public String getApprovalNotes() { return approvalNotes; }
    public String getHumanSignature() { return humanSignature; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
