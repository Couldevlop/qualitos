package com.openlab.qualitos.quality.riskregister.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Une action de mise en œuvre d'une opportunité (ACT-n).
 *
 * <p>Pas une CAPA : une CAPA traite un écart, survenu ou redouté, et ses
 * indicateurs (délai de clôture, récidive) n'ont pas de sens pour un projet
 * d'amélioration. Les mêler aurait faussé le tableau de bord CAPA.
 *
 * <p>Le numéro est unique dans le client, pas dans l'opportunité : « ACT-3 »
 * désigne une seule action, où qu'on la cite.
 */
public final class OpportunityAction {

    public static final int TITLE_MAX = 255;

    private final UUID id;
    private final UUID tenantId;
    private final UUID opportunityId;
    private final int number;
    private String title;
    private LocalDate dueDate;
    private OpportunityActionStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("java:S107") // reconstitution depuis la persistance : un champ par colonne
    public OpportunityAction(UUID id, UUID tenantId, UUID opportunityId, int number, String title,
                             LocalDate dueDate, OpportunityActionStatus status,
                             Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.opportunityId = opportunityId;
        this.number = number;
        this.title = title;
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OpportunityAction open(UUID tenantId, UUID opportunityId, int number, String title,
                                         LocalDate dueDate, OpportunityActionStatus status, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(opportunityId, "opportunityId");
        OpportunityAction a = new OpportunityAction(null, tenantId, opportunityId, number, null,
                null, null, now, now);
        a.revise(title, dueDate, status, now);
        return a;
    }

    public void revise(String newTitle, LocalDate newDueDate, OpportunityActionStatus newStatus,
                       Instant now) {
        this.title = Texts.required("title", newTitle, TITLE_MAX, "L'intitulé est obligatoire.");
        this.dueDate = newDueDate;
        this.status = newStatus == null ? OpportunityActionStatus.TO_START : newStatus;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getOpportunityId() { return opportunityId; }
    public int getNumber() { return number; }
    public String getTitle() { return title; }
    public LocalDate getDueDate() { return dueDate; }
    public OpportunityActionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
