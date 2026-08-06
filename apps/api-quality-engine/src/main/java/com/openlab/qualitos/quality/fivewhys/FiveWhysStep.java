package com.openlab.qualitos.quality.fivewhys;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Un maillon de la chaîne : la réponse au « pourquoi ? » posé au maillon précédent.
 *
 * <p>Une SUITE de pourquoi, et non cinq colonnes figées : cinq est un ordre de
 * grandeur, pas un dogme. Une chaîne de trois qui atteint la cause racine vaut
 * mieux qu'une de cinq qui la dépasse, et certaines défaillances en demandent sept.
 */
@Entity
@Table(name = "five_whys_steps")
@Getter
@Setter
@NoArgsConstructor
public class FiveWhysStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false, updatable = false)
    private FiveWhysAnalysis analysis;

    /** Rang dans la chaîne, à partir de 1. L'ordre EST le sens de la méthode. */
    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
