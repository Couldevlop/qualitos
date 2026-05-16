package com.openlab.qualitos.quality.tenantmodules.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Activation d'un module pour un tenant — agrégat (§10.4).
 *
 * État ↔ transitions (sinon {@link ModuleActivationStateException}) :
 *   TRIAL     → ACTIVE | EXPIRED
 *   ACTIVE    → SUSPENDED | EXPIRED | DISABLED
 *   SUSPENDED → ACTIVE | DISABLED | EXPIRED
 *   EXPIRED   → ∅ (terminal)
 *   DISABLED  → ∅ (terminal — réactivation = nouvelle ligne)
 *
 * trialEndsAt + expiresAt sont des bornes informatives ; un scheduler peut
 * appeler {@link #expireIfDue(Instant)} pour matérialiser le passage à EXPIRED.
 */
public final class ModuleActivation {

    private static final Map<ActivationStatus, Set<ActivationStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ActivationStatus.class);
        ALLOWED.put(ActivationStatus.TRIAL, EnumSet.of(
                ActivationStatus.ACTIVE, ActivationStatus.EXPIRED));
        ALLOWED.put(ActivationStatus.ACTIVE, EnumSet.of(
                ActivationStatus.SUSPENDED, ActivationStatus.EXPIRED, ActivationStatus.DISABLED));
        ALLOWED.put(ActivationStatus.SUSPENDED, EnumSet.of(
                ActivationStatus.ACTIVE, ActivationStatus.DISABLED, ActivationStatus.EXPIRED));
        ALLOWED.put(ActivationStatus.EXPIRED, EnumSet.noneOf(ActivationStatus.class));
        ALLOWED.put(ActivationStatus.DISABLED, EnumSet.noneOf(ActivationStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String moduleCode;
    private ActivationStatus status;
    private BillingTier billingTier;
    private String configurationJson;
    private Instant trialEndsAt;
    private Instant expiresAt;
    private final Instant activatedAt;
    private final UUID activatedBy;
    private Instant statusChangedAt;
    private UUID lastChangedBy;
    private Instant updatedAt;

    public ModuleActivation(UUID id, UUID tenantId, String moduleCode,
                            ActivationStatus status, BillingTier billingTier,
                            String configurationJson,
                            Instant trialEndsAt, Instant expiresAt,
                            Instant activatedAt, UUID activatedBy,
                            Instant statusChangedAt, UUID lastChangedBy,
                            Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId);
        this.moduleCode = Objects.requireNonNull(moduleCode);
        this.status = Objects.requireNonNull(status);
        this.billingTier = Objects.requireNonNull(billingTier);
        this.configurationJson = configurationJson;
        this.trialEndsAt = trialEndsAt;
        this.expiresAt = expiresAt;
        this.activatedAt = Objects.requireNonNull(activatedAt);
        this.activatedBy = Objects.requireNonNull(activatedBy);
        this.statusChangedAt = statusChangedAt != null ? statusChangedAt : activatedAt;
        this.lastChangedBy = lastChangedBy != null ? lastChangedBy : activatedBy;
        this.updatedAt = updatedAt != null ? updatedAt : activatedAt;
    }

    /** Factory : démarre une période d'essai (TRIAL). */
    public static ModuleActivation startTrial(UUID tenantId, String moduleCode,
                                              BillingTier tier, Instant trialEndsAt,
                                              UUID actor, Instant now) {
        if (trialEndsAt == null || !trialEndsAt.isAfter(now)) {
            throw new ModuleActivationStateException("trialEndsAt must be in the future");
        }
        return new ModuleActivation(null, tenantId, moduleCode,
                ActivationStatus.TRIAL, tier, null,
                trialEndsAt, null, now, actor, now, actor, now);
    }

    /** Factory : activation payante directe. */
    public static ModuleActivation activateNow(UUID tenantId, String moduleCode,
                                                BillingTier tier, Instant expiresAt,
                                                UUID actor, Instant now) {
        return new ModuleActivation(null, tenantId, moduleCode,
                ActivationStatus.ACTIVE, tier, null,
                null, expiresAt, now, actor, now, actor, now);
    }

    public void convertTrialToActive(Instant expiresAt, UUID actor, Instant now) {
        ensureTransition(ActivationStatus.ACTIVE);
        this.expiresAt = expiresAt;
        this.trialEndsAt = null;
        change(ActivationStatus.ACTIVE, actor, now);
    }

    public void suspend(UUID actor, Instant now) {
        ensureTransition(ActivationStatus.SUSPENDED);
        change(ActivationStatus.SUSPENDED, actor, now);
    }

    public void resume(UUID actor, Instant now) {
        if (status != ActivationStatus.SUSPENDED) {
            throw new ModuleActivationStateException(
                    "Resume requires SUSPENDED (current: " + status + ")");
        }
        change(ActivationStatus.ACTIVE, actor, now);
    }

    public void disable(UUID actor, Instant now) {
        ensureTransition(ActivationStatus.DISABLED);
        change(ActivationStatus.DISABLED, actor, now);
    }

    public void expire(UUID actor, Instant now) {
        ensureTransition(ActivationStatus.EXPIRED);
        change(ActivationStatus.EXPIRED, actor, now);
    }

    /** Si trial dépassé / expiresAt dépassé, passe en EXPIRED. Idempotent. */
    public boolean expireIfDue(Instant now) {
        if (isTerminal()) return false;
        if (trialEndsAt != null && !now.isBefore(trialEndsAt)) {
            change(ActivationStatus.EXPIRED, activatedBy, now); return true;
        }
        if (expiresAt != null && !now.isBefore(expiresAt)) {
            change(ActivationStatus.EXPIRED, activatedBy, now); return true;
        }
        return false;
    }

    public void changeTier(BillingTier newTier, UUID actor, Instant now) {
        if (isTerminal()) throw new ModuleActivationStateException("Cannot change tier on terminal");
        if (newTier == null) throw new IllegalArgumentException("newTier required");
        this.billingTier = newTier;
        this.lastChangedBy = actor;
        this.updatedAt = now;
    }

    public void configure(String configurationJson, UUID actor, Instant now) {
        if (isTerminal()) throw new ModuleActivationStateException("Cannot configure terminal activation");
        this.configurationJson = configurationJson;
        this.lastChangedBy = actor;
        this.updatedAt = now;
    }

    private void ensureTransition(ActivationStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new ModuleActivationStateException(
                    "Transition " + status + " → " + target + " is not allowed");
        }
    }

    private void change(ActivationStatus target, UUID actor, Instant now) {
        this.status = target;
        this.statusChangedAt = now;
        this.lastChangedBy = actor;
        this.updatedAt = now;
    }

    public boolean isEnabled() {
        return status == ActivationStatus.TRIAL || status == ActivationStatus.ACTIVE;
    }

    public boolean isTerminal() {
        return status == ActivationStatus.EXPIRED || status == ActivationStatus.DISABLED;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getModuleCode() { return moduleCode; }
    public ActivationStatus getStatus() { return status; }
    public BillingTier getBillingTier() { return billingTier; }
    public String getConfigurationJson() { return configurationJson; }
    public Instant getTrialEndsAt() { return trialEndsAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public UUID getActivatedBy() { return activatedBy; }
    public Instant getStatusChangedAt() { return statusChangedAt; }
    public UUID getLastChangedBy() { return lastChangedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
