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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "circle_id", nullable = false, updatable = false)
    private QualityCircle circle;

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
