package com.openlab.qualitos.quality.apqp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
