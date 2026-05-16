package com.openlab.qualitos.quality.dmaic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Mesure individuelle d'une variable process (data point).
 * Plusieurs mesures alimentent les calculs Cp/Cpk + cartes SPC.
 */
@Entity
@Table(name = "dmaic_process_measures")
@Getter
@Setter
@NoArgsConstructor
public class ProcessMeasure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private DmaicProject project;

    @Column(nullable = false)
    private Double value;

    /** Identifiant de sous-groupe (rationnel) — utile pour cartes X-R. Null = mesure individuelle. */
    @Column(name = "subgroup_id", length = 100)
    private String subgroupId;

    /** Référence métier de la mesure (lot, série, batch, ticket…). */
    @Column(name = "source_ref", length = 255)
    private String sourceRef;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "operator_id")
    private UUID operatorId;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        if (this.recordedAt == null) {
            this.recordedAt = now;
        }
    }
}
