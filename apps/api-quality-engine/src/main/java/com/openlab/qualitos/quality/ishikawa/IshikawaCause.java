package com.openlab.qualitos.quality.ishikawa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ishikawa_causes")
@Getter
@Setter
@NoArgsConstructor
public class IshikawaCause {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagram_id", nullable = false, updatable = false)
    private IshikawaDiagram diagram;

    /**
     * Cause parente pour modéliser les sous-causes (5 Pourquoi).
     * null = cause directement rattachée à la branche racine.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private IshikawaCause parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CauseCategory category;

    @Column(nullable = false, length = 500)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Probabilité d'être la cause racine (0..1).
     * Peut être renseignée manuellement ou par scoring IA.
     */
    @Column(name = "root_cause_score")
    private Double rootCauseScore;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<IshikawaCause> children = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
