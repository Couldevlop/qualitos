package com.openlab.qualitos.quality.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_findings")
@Getter
@Setter
@NoArgsConstructor
public class AuditFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private AuditPlan plan;

    /** Item de checklist à l'origine du finding (optionnel). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_item_id")
    private AuditChecklistItem checklistItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FindingType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "clause_ref", length = 100)
    private String clauseRef;

    @Column(name = "photo_url", length = 1024)
    private String photoUrl;

    /** Si une CAPA est créée pour traiter ce finding. */
    @Column(name = "capa_id")
    private UUID capaId;

    @Column(name = "raised_by", nullable = false)
    private UUID raisedBy;

    @Column(name = "raised_at", nullable = false)
    private Instant raisedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.raisedAt == null) {
            this.raisedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
