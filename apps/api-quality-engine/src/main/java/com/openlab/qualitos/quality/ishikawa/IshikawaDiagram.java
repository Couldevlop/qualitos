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
@Table(name = "ishikawa_diagrams")
@Getter
@Setter
@NoArgsConstructor
public class IshikawaDiagram {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "problem_statement", nullable = false)
    private String problemStatement;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IshikawaMode mode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IshikawaStatus status;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    /**
     * Non-conformité d'où part le diagramme, ou {@code null}.
     *
     * <p>Facultatif à dessein : un Ishikawa se tient aussi pour lui-même — revue
     * de processus, atelier de recherche de causes — alors qu'une analyse des
     * 5 Pourquoi part toujours d'un écart constaté. Fixé à la création et jamais
     * ensuite : rattacher après coup un raisonnement à un autre constat que
     * celui qui l'a motivé réécrirait l'histoire.
     */
    @Column(name = "nc_id", updatable = false)
    private UUID ncId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "diagram", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("category ASC, createdAt ASC")
    private List<IshikawaCause> causes = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = IshikawaStatus.DRAFT;
        }
        if (this.mode == null) {
            this.mode = IshikawaMode.SIX_M;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
