package com.openlab.qualitos.quality.fivewhys;

import com.openlab.qualitos.quality.nonconformity.NonConformity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Analyse des 5 Pourquoi, rattachée à une non-conformité (§3.5).
 *
 * <p>La méthode existait dans la plateforme comme sous-causes d'un diagramme
 * Ishikawa. Imbriquée dans un arbre cause-effet, elle n'était ni identifiable ni
 * consultable pour elle-même, et l'on ne pouvait pas partir d'une non-conformité
 * pour la dérouler — c'est pourtant le point de départ naturel.
 */
@Entity
@Table(name = "five_whys_analyses")
@Getter
@Setter
@NoArgsConstructor
public class FiveWhysAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /** L'écart d'où part l'analyse. Une analyse orpheline n'a pas de sujet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nc_id", nullable = false, updatable = false)
    private NonConformity nonConformity;

    /**
     * Énoncé du problème. Repris du titre de la non-conformité à la création —
     * on ne retape pas ce qui est déjà écrit — mais modifiable : formuler
     * précisément le problème fait partie de la méthode.
     */
    @Column(nullable = false, length = 500)
    private String problem;

    /**
     * Cause racine conclue. Nulle tant que l'analyse n'aboutit pas : une analyse
     * en cours et une analyse sans conclusion ne se distingueraient plus.
     */
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

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
