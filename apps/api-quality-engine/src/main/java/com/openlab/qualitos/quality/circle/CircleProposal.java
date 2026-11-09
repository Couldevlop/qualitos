package com.openlab.qualitos.quality.circle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "circle_proposals")
@Getter
@Setter
@NoArgsConstructor
public class CircleProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Cercle d'origine, facultatif : une idée peut être déposée hors cercle (V125). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", updatable = false)
    private QualityCircle circle;

    /**
     * Le client propriétaire.
     *
     * <p>Portée par la ligne et non plus déduite du cercle : depuis la V125 une
     * idée peut n'avoir aucun cercle, et une ligne sans tenant déterminable
     * mélangerait les clients à la première liste.
     */
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** Réunion d'origine (optionnelle : propositions hors-séance autorisées). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id")
    private CircleMeeting meeting;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status;

    @Column(name = "proposed_by", nullable = false)
    private UUID proposedBy;

    /**
     * Le nom de l'auteur, tel que l'annuaire le connaissait au dépôt.
     *
     * <p>Copié à l'écriture et non résolu à l'affichage : le tableau ne doit pas
     * dépendre de la disponibilité de l'annuaire, et un départ ne réécrit pas
     * qui a proposé quoi. Même choix que `reporter_name` sur la non-conformité.
     */
    @Column(name = "proposed_by_name", length = 255)
    private String proposedByName;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "implemented_at")
    private Instant implementedAt;

    @Column(name = "measured_at")
    private Instant measuredAt;

    /** Impact mesuré après mise en oeuvre (texte ou KPI delta). */
    @Column(name = "impact_note", columnDefinition = "TEXT")
    private String impactNote;

    /**
     * D'ou vient l'idee, citee en clair : « Audit 5S zone Atelier B — Seiri note
     * 2/5 le 2026-11-09 ».
     *
     * <p>Une citation plutot qu'une cle etrangere, comme au registre des
     * risques : l'audit se cloture, se corrige, s'archive, et un lien dur se
     * briserait sur une idee qui, elle, reste a traiter. {@code null} pour une
     * idee deposee de la main de quelqu'un.
     *
     * <p>Jamais ecrite par l'appelant : le module qui porte l'idee la construit,
     * sans quoi n'importe qui attribuerait une idee a un audit qui n'existe pas.
     */
    @Column(name = "origin_ref", length = 255, updatable = false)
    private String originRef;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ProposalStatus.PROPOSED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
