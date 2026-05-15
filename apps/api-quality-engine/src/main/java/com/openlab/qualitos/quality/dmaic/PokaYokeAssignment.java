package com.openlab.qualitos.quality.dmaic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pokayoke_assignments")
@Getter
@Setter
@NoArgsConstructor
public class PokaYokeAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private DmaicProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, updatable = false)
    private PokaYokeDevice device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PokaYokeAssignmentStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "implemented_at")
    private Instant implementedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    /** Reduction defaut effective constatee (en %, positif = amelioration). */
    @Column(name = "defect_reduction_pct")
    private Double defectReductionPct;

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
            this.status = PokaYokeAssignmentStatus.PROPOSED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
