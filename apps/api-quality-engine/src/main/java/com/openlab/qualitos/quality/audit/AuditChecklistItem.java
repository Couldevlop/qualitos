package com.openlab.qualitos.quality.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_checklist_items")
@Getter
@Setter
@NoArgsConstructor
public class AuditChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private AuditPlan plan;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    /** Référence à une clause normative (ex: ISO 9001 §8.5.1). */
    @Column(name = "clause_ref", length = 100)
    private String clauseRef;

    @Column(name = "expected_evidence", columnDefinition = "TEXT")
    private String expectedEvidence;

    /** 1=plus important. Permet de calculer un score pondéré du rapport. */
    @Column(nullable = false)
    private Integer weight;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /** Réponse de l'auditeur sur le terrain. */
    @Column(columnDefinition = "TEXT")
    private String response;

    /** true=conforme, false=non conforme, null=non répondu. */
    @Column(name = "conformant")
    private Boolean conformant;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.weight == null) {
            this.weight = 1;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
