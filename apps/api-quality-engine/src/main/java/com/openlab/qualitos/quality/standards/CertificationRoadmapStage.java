package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Une étape de la roadmap de certification d'une adoption ({@link TenantStandard}).
 * Instanciée depuis la trame générique {@link RoadmapTemplate} à l'adoption d'une norme.
 */
@Entity
@Table(name = "certification_roadmap_stages",
        uniqueConstraints = @UniqueConstraint(name = "uk_roadmap_stage_ts_step",
                columnNames = {"tenant_standard_id", "step_number"}))
@Getter
@Setter
@NoArgsConstructor
public class CertificationRoadmapStage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_standard_id", nullable = false, updatable = false)
    private TenantStandard tenantStandard;

    @Column(name = "step_number", nullable = false, updatable = false)
    private int stepNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "typical_duration", length = 60)
    private String typicalDuration;

    @Column(columnDefinition = "TEXT")
    private String deliverables;

    @Column(name = "responsible_role", length = 120)
    private String responsibleRole;

    @Column(name = "involved_modules", length = 255)
    private String involvedModules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageStatus status;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "planned_end_date")
    private LocalDate plannedEndDate;

    @Column(name = "actual_start_date")
    private LocalDate actualStartDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

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
            this.status = StageStatus.NOT_STARTED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
