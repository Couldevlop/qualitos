package com.openlab.qualitos.quality.apqp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Un livrable attendu en sortie de phase.
 *
 * <p>Ce que la phase doit avoir produit avant qu'on passe à la suivante :
 * « AMDEC processus (PFMEA) », « Plan de surveillance de pré-lancement ». La
 * liste vient du manuel AIAG à l'amorçage, puis appartient au client.
 */
@Entity
@Table(name = "apqp_deliverables")
@Getter
@Setter
@NoArgsConstructor
public class ApqpDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * La phase qui l'attend.
     *
     * <p>{@code LAZY} : les listes de phases affichent parfois leurs livrables
     * sans avoir besoin de remonter à la phase depuis chacun d'eux.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private ApqpPhase phase;

    /** Rang dans la liste, à partir de 1. */
    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 500)
    private String label;

    /**
     * Élément du dossier PPAP — l'astérisque du référentiel.
     *
     * <p>Porté par le livrable et non par une liste à part : le dossier PPAP est
     * une vue du cycle, qui appartient au client et qu'il adapte.
     */
    @Column(nullable = false)
    private boolean ppap;

    /** Ce que le formulaire du livrable demande. Voir {@link ApqpDeliverableKind}. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApqpDeliverableKind kind = ApqpDeliverableKind.ATTACHMENT;

    @Column(nullable = false)
    private boolean done;

    @Column(name = "done_at")
    private Instant doneAt;

    /**
     * Qui a coché, pris du jeton — jamais du corps de la requête.
     *
     * <p>L'accepter de l'appelant laisserait attribuer un achèvement à quelqu'un
     * d'autre, et c'est cette attribution que l'auditeur lit.
     */
    @Column(name = "done_by")
    private UUID doneBy;

    @Column(length = 2000)
    private String comment;

    /**
     * Le contenu propre au genre, en JSON.
     *
     * <p>Texte côté Java, {@code jsonb} côté base. Ce qui empêche cette colonne
     * de devenir un fourre-tout n'est pas son type mais le validateur du service,
     * qui n'accepte que deux formes — des mesures, ou des sous-points — selon le
     * genre du livrable.
     */
    @Column(columnDefinition = "jsonb")
    private String data;

    /** Module visé quand le genre est {@code MODULE_LINK}, sinon {@code null}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "linked_kind", length = 32)
    private ApqpLinkedKind linkedKind;

    @Column(name = "linked_id")
    private UUID linkedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant maintenant = Instant.now();
        this.createdAt = maintenant;
        this.updatedAt = maintenant;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
