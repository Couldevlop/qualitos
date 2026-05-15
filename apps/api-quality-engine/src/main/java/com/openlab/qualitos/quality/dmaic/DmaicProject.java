package com.openlab.qualitos.quality.dmaic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dmaic_projects")
@Getter
@Setter
@NoArgsConstructor
public class DmaicProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "problem_statement", columnDefinition = "TEXT")
    private String problemStatement;

    @Column(name = "goal_statement", columnDefinition = "TEXT")
    private String goalStatement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DmaicPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DmaicStatus status;

    @Column(name = "champion_id")
    private UUID championId;

    @Column(name = "black_belt_id", nullable = false)
    private UUID blackBeltId;

    @Column(name = "target_completion_date")
    private LocalDate targetCompletionDate;

    /** Bornes spec process (mesure). */
    @Column(name = "spec_lower_limit")
    private Double specLowerLimit;

    @Column(name = "spec_upper_limit")
    private Double specUpperLimit;

    @Column(name = "spec_target")
    private Double specTarget;

    @Column(name = "spec_unit", length = 50)
    private String specUnit;

    /** ROI annualisé estimé en euros (Champion validation). */
    @Column(name = "estimated_savings_eur")
    private Double estimatedSavingsEur;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("recordedAt ASC")
    private List<ProcessMeasure> measures = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<PokaYokeAssignment> pokaYokeAssignments = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.phase == null) {
            this.phase = DmaicPhase.DEFINE;
        }
        if (this.status == null) {
            this.status = DmaicStatus.ACTIVE;
        }
        if (this.startedAt == null) {
            this.startedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
