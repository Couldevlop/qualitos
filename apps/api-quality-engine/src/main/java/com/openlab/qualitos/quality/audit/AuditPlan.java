package com.openlab.qualitos.quality.audit;

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
@Table(name = "audit_plans")
@Getter
@Setter
@NoArgsConstructor
public class AuditPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * Désignation lisible de l'audit — {@code AUD-2026-0001}.
     *
     * <p>{@code updatable = false} : une référence citée dans un compte rendu de
     * revue ou remise à un organisme certificateur ne peut pas changer ensuite.
     * Ce n'est pas un libellé, c'est une désignation ; le titre, lui, se corrige.
     */
    @Column(nullable = false, length = 40, updatable = false)
    private String reference;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditStatus status;

    /** Référentiel normatif visé (ex: ISO_9001, IATF_16949…). Optionnel. */
    @Column(length = 100)
    private String standard;

    @Column(name = "lead_auditor_id", nullable = false)
    private UUID leadAuditorId;

    @Column(name = "auditee_id")
    private UUID auditeeId;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "report_summary", columnDefinition = "TEXT")
    private String reportSummary;

    /**
     * Destinataire du courriel de rappel d'échéance. Facultatif : sans lui, seule
     * la notification interne part. L'engine ne dispose d'aucun annuaire pour
     * traduire {@code leadAuditorId} en adresse — la déduire enverrait dans le vide.
     */
    @Column(name = "reminder_email", length = 320)
    private String reminderEmail;

    /**
     * Marque d'idempotence du rappel (V106). Posée par un UPDATE conditionnel, elle
     * fait office de verrou entre les répliques : ce n'est pas une trace a posteriori.
     * Setter réduit à la portée du paquet, à dessein : seule la requête atomique du
     * repository doit l'écrire. Un setter public inviterait un appelant à « marquer
     * puis sauvegarder », séquence qui perd la course entre répliques et rouvre le
     * double envoi que cette colonne existe précisément pour fermer.
     */
    @Column(name = "reminder_sent_at")
    @Setter(lombok.AccessLevel.PACKAGE)
    private Instant reminderSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<AuditChecklistItem> checklist = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("raisedAt ASC")
    private List<AuditFinding> findings = new ArrayList<>();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = AuditStatus.PLANNED;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
