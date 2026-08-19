package com.openlab.qualitos.quality.controlplan.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat — le plan de surveillance d'un produit pour une phase donnée.
 *
 * <p>Objet Java nu : ni Spring, ni JPA, ni HTTP. Un plan approuvé est affiché au
 * poste et montré à l'auditeur ; il ne se modifie pas, on en ouvre une révision.
 * L'invariant vit ici et pas seulement dans le service : c'est lui qui rend le
 * document opposable, y compris à un appel interne qui contournerait l'API.
 */
public final class ControlPlan {

    private UUID id;
    private final UUID tenantId;
    private final UUID productId;
    private final ControlPlanPhase phase;
    private String code;
    private int revision;
    private ControlPlanStatus status;
    private UUID ownerUserId;
    private UUID approvedBy;
    private Instant approvedAt;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    private ControlPlan(UUID tenantId, UUID productId, ControlPlanPhase phase, String code,
                        int revision, UUID createdBy, Instant now) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.productId = Objects.requireNonNull(productId, "productId");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.code = requireCode(code);
        this.revision = revision;
        this.status = ControlPlanStatus.DRAFT;
        this.createdBy = Objects.requireNonNull(createdBy, "createdBy");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    public static ControlPlan create(UUID tenantId, UUID productId, ControlPlanPhase phase,
                                     String code, UUID createdBy, Instant now) {
        return new ControlPlan(tenantId, productId, phase, code, 1, createdBy, now);
    }

    /** Reconstruction depuis la persistance : aucune transition rejouée. */
    public static ControlPlan rehydrate(UUID id, UUID tenantId, UUID productId,
                                        ControlPlanPhase phase, String code, int revision,
                                        ControlPlanStatus status, UUID ownerUserId,
                                        UUID approvedBy, Instant approvedAt, UUID createdBy,
                                        Instant createdAt, Instant updatedAt) {
        ControlPlan plan = new ControlPlan(tenantId, productId, phase, code, revision, createdBy, createdAt);
        plan.id = id;
        plan.status = status == null ? ControlPlanStatus.DRAFT : status;
        plan.ownerUserId = ownerUserId;
        plan.approvedBy = approvedBy;
        plan.approvedAt = approvedAt;
        plan.updatedAt = updatedAt;
        return plan;
    }

    private static String requireCode(String code) {
        String trimmed = code == null ? "" : code.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) {
            throw new IllegalArgumentException("Invalid control plan code");
        }
        return trimmed;
    }

    /**
     * Porte d'entrée de toute écriture. Le message nomme la sortie — ouvrir une
     * révision — parce qu'un « conflit d'état » sec laisse l'utilisateur bloqué
     * sans savoir quoi faire.
     */
    public void requireDraft() {
        if (status != ControlPlanStatus.DRAFT) {
            throw new ControlPlanStateException(
                    "Le plan " + code + " est " + status + " : ouvrir une révision pour le modifier");
        }
    }

    public void approve(UUID approver, Instant when) {
        requireDraft();
        this.status = ControlPlanStatus.ACTIVE;
        this.approvedBy = Objects.requireNonNull(approver, "approver");
        this.approvedAt = Objects.requireNonNull(when, "when");
        this.updatedAt = when;
    }

    public void archive() {
        this.status = ControlPlanStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    /**
     * Ouvre la révision suivante. Seul un plan en vigueur en produit une : d'un
     * brouillon il en existe déjà une, et d'un plan archivé la révision suivante
     * est déjà passée.
     */
    public ControlPlan nextRevision(UUID by, Instant now) {
        if (status != ControlPlanStatus.ACTIVE) {
            throw new ControlPlanStateException(
                    "Seul un plan en vigueur ouvre une révision ; celui-ci est " + status);
        }
        ControlPlan next = new ControlPlan(tenantId, productId, phase, code, revision + 1, by, now);
        next.ownerUserId = ownerUserId;
        return next;
    }

    public void rename(String newCode) {
        requireDraft();
        this.code = requireCode(newCode);
        this.updatedAt = Instant.now();
    }

    public void assignOwner(UUID ownerUserId) {
        requireDraft();
        this.ownerUserId = ownerUserId;
        this.updatedAt = Instant.now();
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public ControlPlanPhase getPhase() { return phase; }
    public String getCode() { return code; }
    public int getRevision() { return revision; }
    public ControlPlanStatus getStatus() { return status; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public UUID getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
